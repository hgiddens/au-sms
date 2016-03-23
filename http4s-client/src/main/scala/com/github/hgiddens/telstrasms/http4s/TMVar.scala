package com.github.hgiddens.telstrasms.http4s

import scalaz.concurrent.{ MVar, Task }
import scalaz.effect.IO

/**
 * [[MVar]] with operations in [[Task]] rather than [[IO]].
 *
 * @tparam A the type of the contained value.
 */
private[http4s] sealed trait TMVar[A] {
  def put(a: => A): Task[Unit]
  def take: Task[A]

  final def read: Task[A] =
    for {
      a <- take
      _ <- put(a)
    } yield a

  final def modify[B](f: A => Task[(A, B)]): Task[B] =
    for {
      a <- take
      r <- f(a).onFinish {
        case Some(_) => put(a)
        case _ => Task.now(())
      }
      _ <- put(r._1)
    } yield r._2
}
private[http4s] object TMVar {
  private[this] def task[B](io: => IO[B]): Task[B] =
    Task.delay(io.unsafePerformIO)
  def newTMVar[A](a: A): Task[TMVar[A]] =
    task(MVar.newMVar(a).map(new TMVarImpl(_)))
  def newEmptyTMVar[A]: Task[TMVar[A]] =
    task(MVar.newEmptyMVar[A].map(new TMVarImpl(_)))

  private[this] final class TMVarImpl[A](mvar: MVar[A]) extends TMVar[A] {
    def take = task(mvar.take)
    def put(a: => A) = task(mvar.put(a))
  }
}
