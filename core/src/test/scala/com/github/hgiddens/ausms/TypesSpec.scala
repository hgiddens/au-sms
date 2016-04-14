package com.github.hgiddens.ausms

import Generators._
import io.circe.syntax._
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import scalaz.Equal

object MessageSpec extends Specification with ScalaCheck {
  def smsLength(s: String): Double =
    if (s.forall(Message.restrictedGsm)) (s.length + s.count(Message.restrictedGsmExtension)) * 7 / 8.0
    else s.length * 2.0

  "fromString works on length, not bytes" in prop { s: String =>
    Message.fromString(s) must beSome.iff(smsLength(s) <= 140)
  }

  "apply" should {
    "construct a Message successfully" in {
      Message("this is a message").value ==== "this is a message"
    }
  }
}

object PhoneNumberSpec extends Specification with ScalaCheck {
  "fromString" should {
    "construct a value from a valid Australian mobile number" in {
      val validNumbers = Seq(
        "0400000000",
        "0400 000 000",
        "+61 400 000 000",
        "61400000000",
        " 0400000000 ",
        "(+61) 400 000 000",
        "+(614) 00 000 000"
      )
      val expected = PhoneNumber.fromString("0400000000").get
      foreach(validNumbers) { phone =>
        PhoneNumber.fromString(phone) must beSome(expected)
      }
    }

    "not construct a value from an invalid number" in {
      val invalidNumbers = Seq(
        "132221",
        "64270000000",
        "400000000",
        "0400000000 1"
      )
      foreach(invalidNumbers) { phone =>
        PhoneNumber.fromString(phone) must beNone
      }
    }

    "apply" should {
      "construct a PhoneNumber successfully" in {
        PhoneNumber("0400000000").value ==== "0400000000"
      }
    }
  }

  "codec" should {
    "obey law" in prop { phone: PhoneNumber =>
      phone.asJson.as[PhoneNumber].toOption must beSome(phone)
    }
  }
}
