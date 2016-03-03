package com.github.hgiddens.telstrasms
package http4s

import Http4sSmsClient.Failure
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
import scalaz.Scalaz._
import scalaz.concurrent.Task

private[http4s] object JsonHelpers {
  implicit def jsonEntityDecoder[A: DecodeJson]: EntityDecoder[A] =
    jsonOf
  implicit def jsonEntityEncoder[A: EncodeJson]: EntityEncoder[A] =
    jsonEncoderOf
}

/**
 * [[SmsClient]] that delegates to an underlying [[Client]] instance.
 *
 * @param client the http4s [[Client]] to use for HTTP communication.
 * @param key your Telstra SMS client ID.
 * @param secret your Telstra SMS client secret.
 */
final class Http4sSmsClient(client: Client, key: String, secret: String) extends SmsClient[Task] {
  private[this] val log = getLogger

  def sendMessage(token: Token, to: PhoneNumber, message: Message): Task[MessageId] = {
    val request = Request(Method.POST, uri("https://api.telstra.com/v1/sms/messages")).
      withBody(SendRequest(to, message)).
      putHeaders(
        Authorization(OAuth2BearerToken(token.value)),
        `Content-Type`(MediaType.`application/json`, Some(Charset.`UTF-8`))
      )

    val run = for {
      response <- client(request)
      status = response.status
      _ <- Task.fail(Failure(s"Unexpected response with code $status")).unlessM(status == Status.Accepted)
      sent <- response.as[SendResponse]
      _ <- Task.delay(log.debug(s"Message sent to ${to.shows} with id ${sent.id.shows}"))
    } yield sent.id

    run.onFinish {
      case Some(t) => Task.delay(log.error(t)("Failed to send SMS"))
      case _ => Task.now(())
    }
  }

  def token: Task[Token] = {
    val request = uri("https://api.telstra.com/v1/oauth/token") +?
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

}
object Http4sSmsClient {
  final case class Failure(message: String) extends RuntimeException(message)
}
