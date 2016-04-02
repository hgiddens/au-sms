package com.github.hgiddens.ausms

import org.scalacheck.{ Arbitrary, Gen }
import org.scalacheck.Arbitrary.arbitrary
import scalaz.Scalaz._
import scalaz.scalacheck.ScalaCheckBinding._

object Generators {
  implicit def arbMessage: Arbitrary[Message] =
    Arbitrary(for {
      content <- arbitrary[String]
      message <- Gen.fromOption(Message.fromString(content))
    } yield message)

  implicit def arbMessageId: Arbitrary[MessageId] =
    Arbitrary(Gen.resultOf(MessageId.apply _))

  implicit def arbPhoneNumber: Arbitrary[PhoneNumber] =
    Arbitrary(for {
      digits <- Gen.choose(0, 9).replicateM(8)
      number = s"04${digits.mkString}"
      phoneNumber <- Gen.fromOption(PhoneNumber.fromString(number))
    } yield phoneNumber)
}
