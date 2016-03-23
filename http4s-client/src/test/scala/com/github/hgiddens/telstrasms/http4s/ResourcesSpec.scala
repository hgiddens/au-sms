package com.github.hgiddens.telstrasms
package http4s

import argonaut.Json
import argonaut.Argonaut._
import java.util.Date
import org.specs2.mutable.Specification

object MessageResponseSpec extends Specification {
  val body = Json(
    "messageId" := "E89196B793D930B4",
    "status" := "READ",
    "acknowledgedTimestamp" := "2014-10-26T23:10:00+11:00",
    "content" := "Some response"
  )

  "SmsResponse" should {
    "decode json" in {
      body.as[MessageResponse].toOption must beSome.like {
        case MessageResponse(id, status, date, content) =>
          id ==== MessageId("E89196B793D930B4")
          status ==== MessageResponse.Status.Read
          date ==== new Date(1414325400000L)
          content ==== "Some response"
      }
    }
  }
}
