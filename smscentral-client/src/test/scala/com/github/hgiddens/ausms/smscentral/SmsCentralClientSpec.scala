package com.github.hgiddens.ausms
package smscentral

import Generators._
import java.util.UUID
import org.http4s.{ Request, Response, UrlForm }
import org.http4s.client.{ Client, DisposableResponse }
import org.http4s.dsl._
import org.http4s.scalaxml._
import org.scalacheck.{ Arbitrary, Gen }
import org.scalacheck.Arbitrary.arbitrary
import org.specs2.ScalaCheck
import org.specs2.matcher.{ Matcher, MatcherMacros, TaskMatchers }
import org.specs2.mutable.Specification
import scala.util.Try
import scalaz.Kleisli
import scalaz.concurrent.Task

object SimpleClient {
  def apply(fn: Request => Task[Response]): Client =
    Client(Kleisli(fn).map(new DisposableResponse(_, Task.now(()))), Task.now(()))
}

object SmsCentralClientSpec extends Specification with MatcherMacros with ScalaCheck with TaskMatchers {
  implicit def arbClientConfig: Arbitrary[SmsCentralClient.Config] =
    Arbitrary {
      for {
        username <- arbitrary[String]
        password <- arbitrary[String]
        count <- Gen.choose(1, 11)
        chars <- Gen.listOfN(count, Gen.alphaNumChar)
        originator = chars.mkString
        config <- Gen.fromOption(SmsCentralClient.Config(username, password, originator))
      } yield config
    }

  def beUuidLike: Matcher[String] =
    beSome ^^ ((s: String) => Try(UUID.fromString(s)).toOption)

  def expectParameter(name: String, predicate: String => Boolean)(response: Task[Response]): Client =
    SimpleClient { request =>
      request.as[UrlForm].flatMap { form =>
        form.get(name) match {
          case Seq(actual) if predicate(actual) => response
          case actual => Task.fail(new RuntimeException(s"Expected failed for $name, found $actual"))
        }
      }
    }

  def constantClient(config: SmsCentralClient.Config, response: Task[Response]): SmsCentralClient =
    SmsCentralClient(SimpleClient(_ => response), config)

  "send message" should {
    def successResponse = Ok("0")

    def clientExpectingParameter(config: SmsCentralClient.Config, name: String, predicate: String => Boolean): SmsCentralClient =
      SmsCentralClient(expectParameter(name, predicate)(successResponse), config)

    "send the username from the config" in prop { (config: SmsCentralClient.Config, to: PhoneNumber, message: Message) =>
      val client = clientExpectingParameter(config, "USERNAME", _ == config.username)
      client.sendMessage(to, message) must returnOk
    }

    "send the password from the config" in prop { (config: SmsCentralClient.Config, to: PhoneNumber, message: Message) =>
      val client = clientExpectingParameter(config, "PASSWORD", _ == config.password)
      client.sendMessage(to, message) must returnOk
    }

    "send an action of send" in prop { (config: SmsCentralClient.Config, to: PhoneNumber, message: Message) =>
      val client = clientExpectingParameter(config, "ACTION", _ == "send")
      client.sendMessage(to, message) must returnOk
    }

    "send the originator from the config" in prop { (config: SmsCentralClient.Config, to: PhoneNumber, message: Message) =>
      val client = clientExpectingParameter(config, "ORIGINATOR", _ == config.originator)
      client.sendMessage(to, message) must returnOk
    }

    "send a reference value that's a uuid" in prop { (config: SmsCentralClient.Config, to: PhoneNumber, message: Message) =>
      val client = clientExpectingParameter(config, "REFERENCE", r => Try(UUID.fromString(r)).isSuccess)
      client.sendMessage(to, message) must returnOk
    }

    "send the recipient in the preferred format" in prop { (config: SmsCentralClient.Config, to: PhoneNumber, message: Message) =>
      val client = clientExpectingParameter(config, "RECIPIENT", _ == s"61${to.value.substring(1)}")
      client.sendMessage(to, message) must returnOk
    }

    "send the message text" in prop { (config: SmsCentralClient.Config, to: PhoneNumber, message: Message) =>
      val client = clientExpectingParameter(config, "MESSAGE_TEXT", _ == message.value)
      client.sendMessage(to, message) must returnOk
    }

    "return the message id when successful" in prop { (config: SmsCentralClient.Config, to: PhoneNumber, message: Message) =>
      val client = constantClient(config, successResponse)
      client.sendMessage(to, message) must returnValue(matchA[MessageId].value(beUuidLike))
    }

    "return a failure when an error is returned" in prop { (config: SmsCentralClient.Config, to: PhoneNumber, message: Message) =>
      val client = constantClient(config, Ok("500\r\nFailed"))
      client.sendMessage(to, message).unsafePerformSync must throwA[SmsCentralFailure]
    }

    "return an error if something else goes wrong" in prop { (config: SmsCentralClient.Config, to: PhoneNumber, message: Message) =>
      val client = constantClient(config, InternalServerError())
      client.sendMessage(to, message).unsafePerformSync must throwA[SmsCentralError]
    }
  }

  "message status" should {
    def successResponse =
      Ok {
        <messages>
          <message>
            <datestamp>2016-04-02 17:03:34</datestamp>
            <recipient>61400000000</recipient>
            <status>DELIVRD</status>
            <statusdescription></statusdescription>
            <reference>a87c24b6-8f6f-4e69-b569-ba217da96769</reference>
          </message>
        </messages>
      }

    def clientExpectingParameter(config: SmsCentralClient.Config, name: String, predicate: String => Boolean): SmsCentralClient =
      SmsCentralClient(expectParameter(name, predicate)(successResponse), config)

    "send the request to the right place" in prop { (config: SmsCentralClient.Config, message: MessageId) =>
      val underlying = SimpleClient {
        case POST -> Root / "api" / "v3.2" / "checkstatus" => successResponse
      }
      val client = SmsCentralClient(underlying, config)
      client.messageStatus(message) must returnOk
    }

    "send the username from the config" in prop { (config: SmsCentralClient.Config, message: MessageId) =>
      val client = clientExpectingParameter(config, "USERNAME", _ == config.username)
      client.messageStatus(message) must returnOk
    }

    "send the password from the config" in prop { (config: SmsCentralClient.Config, message: MessageId) =>
      val client = clientExpectingParameter(config, "PASSWORD", _ == config.password)
      client.messageStatus(message) must returnOk
    }

    "send the message id" in prop { (config: SmsCentralClient.Config, message: MessageId) =>
      val client = clientExpectingParameter(config, "REFERENCE", _ == message.value)
      client.messageStatus(message) must returnOk
    }

    "return the message status on success" in prop { (config: SmsCentralClient.Config, message: MessageId) =>
      val client = constantClient(config, successResponse)
      client.messageStatus(message) must returnValue(DeliveryStatus.Delivered)
    }

    "return an error if no message is found" in prop { (config: SmsCentralClient.Config, message: MessageId) =>
      val client = constantClient(config, Ok(<messages/>))
      client.messageStatus(message).unsafePerformSync must throwA[SmsCentralError]
    }

    "return a failure if an error is received" in prop { (config: SmsCentralClient.Config, message: MessageId) =>
      val failureResponse = Ok {
        <error>
          <errorcode>511</errorcode>
          <errormessage>Username or Password incorrect</errormessage>
        </error>
      }
      val client = constantClient(config, failureResponse)
      client.messageStatus(message).unsafePerformSync must throwA[SmsCentralFailure]
    }

    "return an error if anything else goes wrong" in prop { (config: SmsCentralClient.Config, message: MessageId) =>
      val client = constantClient(config, InternalServerError("Error"))
      client.messageStatus(message).unsafePerformSync must throwA[SmsCentralError]
    }
  }
}
