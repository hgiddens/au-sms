package com.github.hgiddens.ausms

/**
 * A client for sending SMS messages.
 *
 * @tparam F the context in which results will be returned, e.g. [[scala.concurrent.Future]]
 */
trait SmsClient[F[_]] {
  /**
   * Sends a SMS.
   *
   * @param to the recipient of the message.
   * @param message the body of the message.
   * @return the ID of the sent message.
   */
  def sendMessage(to: PhoneNumber, message: Message): F[MessageId]

  /**
   * Determines the status of a previously sent message.
   *
   * @param message the ID of the sent message.
   */
  def messageStatus(message: MessageId): F[DeliveryStatus]
}
