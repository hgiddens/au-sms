package com.github.hgiddens.ausms
package telstra

import argonaut.{ DecodeJson, DecodeResult, EncodeJson, Json }
import argonaut.Argonaut._
import java.time.OffsetDateTime
import java.util.Date
import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }
import scalaz.{ Equal, Show }
import scalaz.Scalaz._

private[ausms] final case class SendRequest(to: PhoneNumber, body: Message)
private[ausms] object SendRequest {
  implicit def encodeJson: EncodeJson[SendRequest] =
    EncodeJson(request => Json("to" := request.to.value, "body" := request.body.value))
}

private[ausms] final case class SendResponse(id: MessageId)
private[ausms] object SendResponse {
  implicit def decodeJson: DecodeJson[SendResponse] =
    DecodeJson(c => for {
      id <- (c --\ "messageId").as[MessageId]
    } yield apply(id))
}

private[ausms] final case class TokenResponse(accessToken: String, duration: Duration) {
  def asToken(base: Date): Token =
    Token(accessToken, new Date(base.getTime + duration.toMillis))
}
private[ausms] object TokenResponse {
  implicit def decodeJson: DecodeJson[TokenResponse] =
    DecodeJson(c => for {
      accessToken <- (c --\ "access_token").as[String]
      expiresStr <- (c --\ "expires_in").as[String]
      expires <- Try(expiresStr.toInt) match {
        case Success(s) => DecodeResult.ok(s.seconds)
        case Failure(_) => DecodeResult.fail("Bad token expiry", (c --\ "expires_in").history)
      }
    } yield apply(accessToken, expires))
}

/**
 * The response the web hook receives when someone replies to a message.
 *
 * @param id the ID of the message this response is to.
 * @param status the status of the message.
 * @param acknowledged when the message was received.
 * @param content the content of the response.
 */
final case class MessageResponse(id: MessageId, status: MessageResponse.Status, acknowledged: Date, content: String)
object MessageResponse {
  sealed trait Status
  object Status {
    /** The message has been received by the network or handset. */
    case object Read extends Status
    case object Undeliverable extends Status

    implicit def equal: Equal[Status] =
      Equal.equalA

    implicit def show: Show[Status] =
      Show.showA
  }

  private[this] def parseDate(s: String): Option[Date] =
    Try(Date.from(OffsetDateTime.parse(s).toInstant)).toOption

  implicit def decodeJson: DecodeJson[MessageResponse] =
    DecodeJson(c => for {
      id <- (c --\ "messageId").as[MessageId]
      statusString <- (c --\ "status").as[String]
      status <- statusString match {
        case "READ" => DecodeResult.ok(Status.Read)
        case "UNDVBL" => DecodeResult.ok(Status.Undeliverable)
        case s => DecodeResult.fail(s"Unknown status '$s'", (c --\ "status").history)
      }
      dateString <- (c --\ "acknowledgedTimestamp").as[String]
      date <- parseDate(dateString) match {
        case Some(date) => DecodeResult.ok(date)
        case _ => DecodeResult.fail(s"Unable to parse timestamp '$dateString'", (c --\ "acknowledgedTimestamp").history)
      }
      message <- (c --\ "content").as[String]
    } yield apply(id, status, date, message))
}

final case class MessageStatusResponse(status: DeliveryStatus)
object MessageStatusResponse {
  private[this] def parseStatus: PartialFunction[String, DeliveryStatus] = {
    case "PEND" => DeliveryStatus.Pending
    case "SENT" => DeliveryStatus.Sent
    case "DELIVRD" => DeliveryStatus.Delivered
    case "READ" => DeliveryStatus.Read
  }

  implicit def decodeJson: DecodeJson[MessageStatusResponse] =
    DecodeJson(c => for {
      statusString <- (c --\ "status").as[String]
      status <- parseStatus.lift(statusString).cata(
        DecodeResult.ok,
        DecodeResult.fail(s"Unknown status '$statusString'", (c --\ "status").history)
      )
    } yield MessageStatusResponse(status))
}
