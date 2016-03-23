package com.github.hgiddens.telstrasms
package http4s

import Generators._
import java.util.Date
import org.specs2.ScalaCheck
import org.specs2.matcher.{ MatcherMacros, TaskMatchers }
import org.specs2.mutable.Specification

import scalaz.concurrent.Task

object Http4sSmsClientSpec extends Specification with MatcherMacros with ScalaCheck with TaskMatchers {
  "freshen" should {
    "refresh the token when it's expired" in {
      (for {
        client <- Http4sSmsClient(TestClient, TestClient.key, TestClient.secret)
        _ <- client.freshen(_ => Task.now(()))
        token <- client.currentToken.read
      } yield token) must returnValue(matchA[Token].value(TestClient.accessToken))
    }

    "pass the new token to the callback when it's expired" in {
      (for {
        client <- Http4sSmsClient(TestClient, TestClient.key, TestClient.secret)
        token <- client.freshen(Task.now)
      } yield token) must returnValue(matchA[Token].value(TestClient.accessToken))
    }

    "not refresh the token when it's valid" in {
      val validToken = "distinct valid token"
      (for {
        now <- Task.delay(new Date)
        token <- TMVar.newTMVar(Token(validToken, new Date(now.getTime + 3600000)))
        client = new Http4sSmsClient(TestClient, TestClient.key, TestClient.secret, token)
        _ <- client.freshen(_ => Task.now(()))
        updated <- client.currentToken.read
      } yield updated) must returnValue(matchA[Token].value(validToken))
    }
  }

  "requesting a token" should {
    "return a token constructed from the returned access token" in {
      (for {
        client <- Http4sSmsClient(TestClient, TestClient.key, TestClient.secret)
        token <- client.token
      } yield token) must returnValue(matchA[Token].value(TestClient.accessToken))
    }
  }

  "sending a message" should {
    "return the message id for the sent message" in prop { (phoneNumber: PhoneNumber, message: Message) =>
      (for {
        client <- Http4sSmsClient(TestClient, TestClient.key, TestClient.secret)
        messageId <- client.sendMessage(phoneNumber, message)
      } yield messageId) must returnValue(be_===(MessageId(TestClient.messageId)))
    }
  }

  "check a message's status" should {
    "return the status for the sent message" in {
      (for {
        client <- Http4sSmsClient(TestClient, TestClient.key, TestClient.secret)
        messageId = MessageId(TestClient.messageId)
        status <- client.messageStatus(messageId)
      } yield status) must returnValue(DeliveryStatus.Delivered)
    }
  }
}
