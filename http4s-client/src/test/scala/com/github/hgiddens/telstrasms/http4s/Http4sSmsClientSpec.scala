package com.github.hgiddens.telstrasms
package http4s

import Generators._
import java.util.Date
import org.specs2.ScalaCheck
import org.specs2.matcher.MatcherMacros
import org.specs2.mutable.Specification

object Http4sSmsClientSpec extends Specification with MatcherMacros with ScalaCheck {
  "requesting a token" should {
    "return a token constructed from the returned access token" in {
      val client = new Http4sSmsClient(TestClient, TestClient.key, TestClient.secret)
      client.token.run must matchA[Token].value(TestClient.accessToken)
    }
  }

  "sending a message" should {
    "return the message id for the sent message" in prop { (phoneNumber: PhoneNumber, message: Message) =>
      val client = new Http4sSmsClient(TestClient, TestClient.key, TestClient.secret)
      client.sendMessage(Token(TestClient.accessToken, new Date), phoneNumber, message).run ==== MessageId(TestClient.messageId)
    }
  }
}
