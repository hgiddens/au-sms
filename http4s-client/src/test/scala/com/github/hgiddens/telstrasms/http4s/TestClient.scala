package com.github.hgiddens.telstrasms
package http4s

import JsonHelpers._
import argonaut.Json
import argonaut.Argonaut._
import org.http4s.{ Charset, MediaType, OAuth2BearerToken, Request }
import org.http4s.client.Client
import org.http4s.dsl._
import org.http4s.headers.{ Authorization, `Content-Type` }
import scalaz.concurrent.Task

private[http4s] object TestClient extends Client {
  val key = "key"
  val secret = "secret"
  val accessToken = "g85MslKJGuDIPhOklSgNLbWFPu5g"
  val messageId = "48E48E5EB8125001D6154CADFA19DA48"

  private[this] val jsonContentType = `Content-Type`(MediaType.`application/json`, Some(Charset.`UTF-8`))

  def prepare(req: Request) =
    req match {
      case GET -> Root / "v1" / "oauth" / "token" =>
        Ok().withBody(Json("access_token" := accessToken, "expires_in" := "3599"))

      case POST -> Root / "v1" / "sms" / "messages" =>
        (req.headers, req.headers) match {
          case (Authorization(Authorization(OAuth2BearerToken(`accessToken`))), `Content-Type`(`jsonContentType`)) =>
            Accepted().withBody(Json("messageId" := messageId))
        }
    }

  def shutdown =
    Task.now(())
}
