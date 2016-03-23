package com.github.hgiddens.ausms
package telstra

import TelstraSmsClient.Failure
import JsonHelpers._
import argonaut.{ DecodeJson, EncodeJson }
import java.util.Date
import org.http4s.{ Charset, EntityDecoder, EntityEncoder, MediaType, Method, OAuth2BearerToken, Request, Status }
import org.http4s.Http4s._
import org.http4s.Uri.uri
import org.http4s.argonaut.{ jsonEncoderOf, jsonOf }
import org.http4s.headers.{ Authorization, `Content-Type` }
import org.http4s.client.Client
import org.log4s.getLogger
import scala.concurrent.duration._
import scalaz.Scalaz._
import scalaz.concurrent.Task

private[telstra] object JsonHelpers {
  implicit def jsonEntityDecoder[A: DecodeJson]: EntityDecoder[A] =
    jsonOf
  implicit def jsonEntityEncoder[A: EncodeJson]: EntityEncoder[A] =
    jsonEncoderOf
}

/**
 * An authentication token for the SMS API.
 *
 * @param value the token's value.
 * @param expires when the token will expire.
 */
final case class Token(value: String, expires: Date)

/**
 * [[SmsClient]] for the [[https://dev.telstra.com/content/sms-getting-started Telstra SMS API]].
 *
 * Delegates to an underlying [[Client]] instance.
 *
 * @param client the http4s [[Client]] to use for HTTP communication.
 * @param key your Telstra SMS client ID.
 * @param secret your Telstra SMS client secret.
 * @param currentToken storage for the current token. Must not be empty.
 */
final class TelstraSmsClient(client: Client, key: String, secret: String, private[telstra] val currentToken: TMVar[Token]) extends SmsClient[Task] {
  private[this] val log = getLogger
  private[this] val margin = 1.minute
  private[this] val oauthBase = uri("https://api.telstra.com/v1/oauth")
  private[this] val smsBase = uri("https://api.telstra.com/v1/sms")

  private[telstra] def freshen[A](body: Token => Task[A]): Task[A] =
    for {
      now <- Task.delay(new Date)
      current <- currentToken.modify { current =>
        val refresh = (current.expires.getTime - now.getTime) < margin.toMillis
        (refresh ? token | Task.now(current)).fpair
      }
      result <- body(current)
    } yield result

  private[telstra] def token: Task[Token] = {
    val request = oauthBase / "token" +?
      ("client_id", key) +?
      ("client_secret", secret) +?
      ("grant_type", "client_credentials") +?
      ("scope", "SMS")

    val run = for {
      now <- Task.delay(new Date)
      response <- client(request)
      status = response.status
      _ <- Task.fail(Failure(s"Unexpected response with code $status")).unlessM(status == Status.Ok)
      tokenResponse <- response.as[TokenResponse]
      _ <- Task.delay(log.debug(s"Token refreshed, expiring in ${tokenResponse.duration.toSeconds}s"))
    } yield tokenResponse.asToken(now)

    run.onFinish {
      case Some(t) => Task.delay(log.error(t)("Failed to refresh access token"))
      case _ => Task.now(())
    }
  }

  def sendMessage(to: PhoneNumber, message: Message): Task[MessageId] = {
    def request(token: Token) =
      Request(Method.POST, smsBase / "messages").
        withBody(SendRequest(to, message)).
        putHeaders(
          Authorization(OAuth2BearerToken(token.value)),
          `Content-Type`(MediaType.`application/json`, Some(Charset.`UTF-8`))
        )

    def run(token: Token) =
      for {
        response <- client(request(token))
        status = response.status
        _ <- Task.fail(Failure(s"Unexpected response with code $status")).unlessM(status == Status.Accepted)
        sent <- response.as[SendResponse]
        _ <- Task.delay(log.debug(s"Message sent to ${to.shows} with id ${sent.id.shows}"))
      } yield sent.id

    freshen(run).onFinish {
      case Some(t) => Task.delay(log.error(t)("Failed to send SMS"))
      case _ => Task.now(())
    }
  }

  def messageStatus(message: MessageId): Task[DeliveryStatus] = {
    def request(token: Token) =
      // TODO: path values need encoding
      Request(Method.GET, smsBase / "messages" / message.value).
        putHeaders(Authorization(OAuth2BearerToken(token.value)))

    def run(token: Token) =
      for {
        response <- client(request(token))
        _ <- Task.fail(Failure(s"Unexpected response with code ${response.status}")).unlessM(response.status == Status.Ok)
        status <- response.as[MessageStatusResponse]
        _ <- Task.delay(log.debug(s"Message status of ${message.shows} is ${status.status.shows}"))
      } yield status.status

    freshen(run).onFinish {
      case Some(t) => Task.delay(log.error(t)("Failed to check status of SMS"))
      case _ => Task.now(())
    }
  }
}
object TelstraSmsClient {
  final case class Failure(message: String) extends RuntimeException(message)

  def apply(client: Client, key: String, secret: String): Task[TelstraSmsClient] =
    for {
      // Supply an initially invalid token so the first request refreshes it.
      token <- TMVar.newTMVar(Token("", new Date(0)))
    } yield new TelstraSmsClient(client, key, secret, token)
}
