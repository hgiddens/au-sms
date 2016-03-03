package com.github.hgiddens.telstrasms

import argonaut.DecodeJson
import java.util.Date
import monocle.{ Iso, Prism }
import monocle.macros.GenIso
import scala.reflect.macros.blackbox
import scalaz.{ Equal, Show }
import scalaz.Scalaz._

/**
 * The body of a SMS message.
 *
 * @param value the message content. Has at most [[com.github.hgiddens.telstrasms.Message.maxLength]] characters.
 */
final class Message private[Message] (val value: String) extends AnyVal {
  override def toString =
    s"Message($value)"
}
object Message {
  def apply(s: String): Message = macro macros.messageLiteralMacro

  val maxLength = 160
  def fromString(s: String): Option[Message] =
    (s.length <= maxLength).option(new Message(s))

  object macros {
    def messageLiteralMacro(c: blackbox.Context)(s: c.Expr[String]): c.Expr[Message] = {
      import c.universe._
      s.tree match {
        case Literal(Constant(s: String)) =>
          fromString(s) match {
            case Some(_) => c.Expr(q"com.github.hgiddens.telstrasms.Message.fromString($s).get")
            case _ => c.abort(c.enclosingPosition, s"'$s' is too long for a SMS message ($maxLength chars)")
          }

        case _ =>
          c.abort(c.enclosingPosition, s"Only literal strings are supported")
      }
    }
  }

  def message: Prism[String, Message] =
    Prism[String, Message](fromString)(_.value)

  implicit def equal: Equal[Message] =
    Equal.equalA

  implicit def show: Show[Message] =
    Show.showA
}

/**
 * A SMS message ID.
 *
 * Message IDs are returned when a message is sent and can be used to correlate
 * responses received with the messages they are in response to.
 *
 * @param value the message ID.
 */
final case class MessageId(value: String) extends AnyVal
object MessageId {
  def _string: Iso[MessageId, String] =
    GenIso[MessageId, String]

  implicit def decodeJson: DecodeJson[MessageId] =
    DecodeJson(c => c.focus.as[String].map(apply))

  implicit def equal: Equal[MessageId] =
    Equal.equalA

  implicit def show: Show[MessageId] =
    Show.showA
}

/**
 * An Australian mobile phone number.
 *
 * @param value the phone number, canonicalised to "04xxxxxxxx".
 */
final class PhoneNumber private[PhoneNumber] (val value: String) extends AnyVal {
  override def toString =
    s"PhoneNumber($value)"
}
object PhoneNumber {
  def apply(s: String): PhoneNumber = macro macros.phoneNumberLiteral

  object macros {
    def phoneNumberLiteral(c: blackbox.Context)(s: c.Expr[String]): c.Expr[PhoneNumber] = {
      import c.universe._
      s.tree match {
        case Literal(Constant(s: String)) =>
          fromString(s) match {
            case Some(_) => c.Expr(q"com.github.hgiddens.telstrasms.PhoneNumber.fromString($s).get")
            case _ => c.abort(c.enclosingPosition, s"'$s' is not a valid phone number")
          }

        case _ =>
          c.abort(c.enclosingPosition, s"Only literal strings are supported")
      }
    }
  }

  private[this] val Rx = """(?:0|\+?61)4(\d{8})""".r
  def fromString(s: String): Option[PhoneNumber] =
    s.filter(_.isDigit) match {
      case Rx(suffix) => Some(new PhoneNumber("04" + suffix))
      case _ => None
    }

  def phoneNumber: Prism[String, PhoneNumber] =
    Prism[String, PhoneNumber](fromString)(_.value)

  implicit def equal: Equal[PhoneNumber] =
    Equal.equalA

  implicit def show: Show[PhoneNumber] =
    Show.showA
}

/**
 * An authentication token for the SMS API.
 *
 * @param value the token's value.
 * @param expires when the token will expire.
 */
final case class Token(value: String, expires: Date)
