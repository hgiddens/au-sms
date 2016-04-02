package com.github.hgiddens.ausms
package telstra

import argonaut.{ DecodeJson, DecodeResult, EncodeJson, Json }
import argonaut.Argonaut._
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
