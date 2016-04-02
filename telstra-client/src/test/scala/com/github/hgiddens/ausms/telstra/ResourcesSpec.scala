package com.github.hgiddens.ausms
package telstra

import argonaut.Json
import argonaut.Argonaut._
import org.specs2.matcher.{ Matcher, MatcherMacros }
import org.specs2.mutable.Specification

object MessageStatusResponseSpec extends Specification with MatcherMacros {
  def body(status: String) =
    Json(
      "to" := "0400000000",
      "receivedTimestamp" := "2015-02-05T14:10:14+11:00",
      "sentTimestamp" := "2015-02-05T14:10:12+11:00",
      "status" := status
    )

  def beResponseWithStatus(status: DeliveryStatus): Matcher[MessageStatusResponse] =
    matchA[MessageStatusResponse].status(status)

  "Decoding JSON" should {
    "decode PEND status" in {
      body("PEND").as[MessageStatusResponse].toEither must beRight(beResponseWithStatus(DeliveryStatus.Pending))
    }

    "decode SENT status" in {
      body("SENT").as[MessageStatusResponse].toEither must beRight(beResponseWithStatus(DeliveryStatus.Sent))
    }

    "decode DELIVRD status" in {
      body("DELIVRD").as[MessageStatusResponse].toEither must beRight(beResponseWithStatus(DeliveryStatus.Delivered))
    }

    "decode READ status" in {
      body("READ").as[MessageStatusResponse].toEither must beRight(beResponseWithStatus(DeliveryStatus.Read))
    }
  }
}
