package com.github.hgiddens.ausms
package telstra

import JsonHelpers._
import io.circe.Json
import io.circe.syntax._
import org.http4s.{ Charset, MediaType, OAuth2BearerToken, Request, Response }
import org.http4s.client.{ Client, DisposableResponse }
import org.http4s.dsl._
import org.http4s.headers.{ Authorization, `Content-Type` }
import scalaz.Kleisli
import scalaz.concurrent.Task

private[telstra] object TestClient {
  val key = "key"
  val secret = "secret"
  val accessToken = "g85MslKJGuDIPhOklSgNLbWFPu5g"
  val messageId = "48E48E5EB8125001D6154CADFA19DA48"

  private val jsonContentType = `Content-Type`(MediaType.`application/json`, Some(Charset.`UTF-8`))

  private def perform(req: Request): Task[Response] =
    req match {
      case GET -> Root / "v1" / "oauth" / "token" =>
        Ok().withBody(Json.obj("access_token" -> accessToken.asJson, "expires_in" -> "3599".asJson))

      case POST -> Root / "v1" / "sms" / "messages" =>
        (req.headers, req.headers) match {
          case (Authorization(Authorization(OAuth2BearerToken(`accessToken`))), `Content-Type`(`jsonContentType`)) =>
            Accepted().withBody(Json.obj("messageId" -> messageId.asJson))
        }

      case GET -> Root / "v1" / "sms" / "messages" / `messageId` =>
        req.headers match {
          case Authorization(Authorization(OAuth2BearerToken(`accessToken`))) =>
            Ok().withBody(Json.obj(
              "to" -> "0400000000".asJson,
              "receivedTimestamp" -> "2015-02-05T14:10:14+11:00".asJson,
              "sentTimestamp" -> "2015-02-05T14:10:12+11:00".asJson,
              "status" -> "DELIVRD".asJson
            ))
        }
    }

  def client: Client =
    Client(Kleisli(perform _).map(new DisposableResponse(_, Task.now(()))), Task.now(()))
}
