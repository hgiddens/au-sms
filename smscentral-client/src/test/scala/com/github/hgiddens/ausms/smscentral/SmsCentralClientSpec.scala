package com.github.hgiddens.ausms
package smscentral

import Generators._
import java.io.StringReader
import java.util.UUID
import org.http4s.{ Request, UrlForm }
import org.http4s.client.Client
import org.http4s.dsl._
import org.specs2.ScalaCheck
import org.specs2.matcher.{ Matcher, MatcherMacros, TaskMatchers }
import org.specs2.mutable.Specification
import scala.reflect.ClassTag
import scala.util.Try
import scalaz.{ Kleisli, Monoid, NonEmptyList, ValidationNel }
import scalaz.Scalaz._
import scalaz.concurrent.Task

private[smscentral] final class TestClient(config: SmsCentralClient.Config) extends Client {
  private[this]type Checker = Kleisli[ValidationNel[String, ?], UrlForm, Unit]
  private[this] val Checker = Kleisli.apply[ValidationNel[String, ?], UrlForm, Unit] _
  private[this] implicit def checkerMonoid: Monoid[Checker] =
    Kleisli.kleisliMonoid[ValidationNel[String, ?], UrlForm, Unit]

  private[this] def check(parameter: String): Checker =
    Checker(form => form.getFirst(parameter).toSuccessNel(s"$parameter required but absent").void)
  private[this] def check(parameter: String, predicate: String => Boolean): Checker =
    Checker(form => form.getFirst(parameter).filter(predicate).toSuccessNel(s"$parameter value mismatch").void)

  def prepare(request: Request) =
    request match {
      case POST -> Root / "api" / "v3.2" =>
        request.as[UrlForm].flatMap { form =>
          val validate =
            check("USERNAME", _ === config.username) |+|
              check("PASSWORD", _ === config.password) |+|
              check("ACTION", _ === "send") |+|
              check("ORIGINATOR", _ === config.originator) |+|
              check("RECIPIENT", _.startsWith("614")) |+|
              check("MESSAGE_TEXT", _ /== "fail") |+|
              check("REFERENCE")

          val body = validate.run(form).fold(
            errors => s"500 ${errors.list.mkString(", ")}",
            _ => "0"
          )
          // Wrap body in a string reader to pick up on attempts to decode the body multiple times.
          Ok(new StringReader(body))
        }

      case POST -> Root / "api" / "v3.2" / "checkstatus" =>
        request.as[UrlForm].flatMap { form =>
          val validate =
            check("USERNAME", _ === config.username) |+|
              check("PASSWORD", _ === config.password) |+|
              check("REFERENCE", _ /== "fail")

          def foundBody =
            <messages>
              <message>
                <datestamp>2016-04-02 17:03:34</datestamp>
                <recipient>61400000000</recipient>
                <status>DELIVRD</status>
                <statusdescription></statusdescription>
                <reference>a87c24b6-8f6f-4e69-b569-ba217da96769</reference>
              </message>
            </messages>

          def errorBody(errors: NonEmptyList[String]) =
            <error>
              <errorcode>511</errorcode>
              <errormessage>Username or Password incorrect</errormessage>
            </error>

          val body = validate.run(form).fold(errorBody, _ => foundBody)
          Ok(new StringReader(body.toString))
        }
    }

  def shutdown =
    Task.now(())
}

object SmsCentralClientSpec extends Specification with MatcherMacros with ScalaCheck with TaskMatchers {
  def config = SmsCentralClient.Config("test username", "test password", "testorig").get
  def underlying = new TestClient(config)
  def client = new SmsCentralClient(underlying, config)

  def beUuidLike: Matcher[String] =
    beSome ^^ ((s: String) => Try(UUID.fromString(s)).toOption)

  def failWith[E <: Throwable: ClassTag]: Matcher[Task[Any]] =
    (a: Task[Any]) => a.run must throwA[E]

  "send message" should {
    "return the message id for the message sent" in prop { (phoneNumber: PhoneNumber, message: Message) =>
      (message.value /== "fail") ==> {
        client.sendMessage(phoneNumber, message) must returnValue(matchA[MessageId].value(beUuidLike))
      }
    }

    "fail when message sending fails" in prop { phoneNumber: PhoneNumber =>
      val message = Message("fail")
      client.sendMessage(phoneNumber, message) must failWith[SmsCentralException]
    }
  }

  "checking status" should {
    "respond with delivery details" in prop { message: MessageId =>
      client.messageStatus(message) must returnValue(DeliveryStatus.Delivered)
    }
  }
}
