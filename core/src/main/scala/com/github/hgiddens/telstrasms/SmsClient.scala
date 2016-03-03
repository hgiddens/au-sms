package com.github.hgiddens.telstrasms

/**
 * A client for the [[https://dev.telstra.com/content/sms-getting-started Telstra SMS API]].
 *
 * @tparam F the context in which results will be returned, e.g. [[scala.concurrent.Future]]
 */
trait SmsClient[F[_]] {
  /**
   * Sends a SMS.
   *
   * token is assumed to be valid and will not be refreshed if it has, for example,
   * expired.
   *
   * @param token an authentication token.
   * @param to the recipient of the message.
   * @param message the body of the message.
   * @return the ID of the sent message.
   */
  def sendMessage(token: Token, to: PhoneNumber, message: Message): F[MessageId]

  /**
   * Requests a new authentication token.
   *
   * @return a new authentication token.
   */
  def token: F[Token]
}
