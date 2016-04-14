package com.github.hgiddens.ausms
package telstra

import cats.data.Xor
import io.circe.{ Decoder, DecodingFailure, Encoder, Json }
import io.circe.syntax._
import java.util.Date
import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }
import scalaz.{ Equal, Show }
import scalaz.Scalaz._

private[ausms] final case class SendRequest(to: PhoneNumber, body: Message)
private[ausms] object SendRequest {
  implicit def encoder: Encoder[SendRequest] =
    Encoder.instance { request =>
      Json.obj(
        "to" -> request.to.value.asJson,
        "body" -> request.body.value.asJson
      )
    }
}

private[ausms] final case class SendResponse(id: MessageId)
private[ausms] object SendResponse {
  implicit def decoder: Decoder[SendResponse] =
    Decoder.instance(c => for {
      id <- c.get[MessageId]("messageId")
    } yield apply(id))
}

private[ausms] final case class TokenResponse(accessToken: String, duration: Duration) {
  def asToken(base: Date): Token =
    Token(accessToken, new Date(base.getTime + duration.toMillis))
}
private[ausms] object TokenResponse {
  implicit def decoder: Decoder[TokenResponse] =
    Decoder.instance(c => for {
      accessToken <- c.get[String]("access_token")
      expiresStr <- c.get[String]("expires_in")
      expires <- Try(expiresStr.toInt) match {
        case Success(s) => Xor.right(s.seconds)
        case Failure(_) => Xor.left(DecodingFailure("Bad token expiry", c.downField("expires_in").history))
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

  implicit def decoder: Decoder[MessageStatusResponse] =
    Decoder.instance(c => for {
      statusString <- c.get[String]("status")
      status <- parseStatus.lift(statusString).cata(
        Xor.right,
        Xor.left(DecodingFailure(s"Unknown status '$statusString'", c.downField("status").history))
      )
    } yield apply(status))
}
