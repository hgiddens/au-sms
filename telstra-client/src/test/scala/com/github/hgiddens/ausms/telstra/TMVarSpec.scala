package com.github.hgiddens.ausms
package telstra

import org.scalacheck.{ Arbitrary, Cogen, Gen }
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.commands.Commands
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import scala.util.{ Failure, Success, Try }
import scalaz.Equal
import scalaz.concurrent.Task
import scalaz.Scalaz._

final class TMVarCommands[A: Arbitrary: Cogen: Equal, B: Arbitrary: Equal] extends Commands {
  type State = A
  type Sut = TMVar[A]

  def canCreateNewSut(newState: State, initSuts: Traversable[State], runningSuts: Traversable[Sut]) =
    true
  def destroySut(sut: Sut) =
    ()
  def genCommand(state: State) = {
    val arbModify = Arbitrary.arbFunction1[A, Either[Throwable, (A, B)]](Arbitrary.arbEither, Cogen[A])
    Gen.oneOf(arbModify.arbitrary.map(Modify.apply), Gen.const(Read))
  }
  def genInitialState =
    arbitrary[A]
  def initialPreCondition(state: State) =
    true
  def newSut(state: State) =
    TMVar.newTMVar(state).unsafePerformSync

  private[this] case object Read extends SuccessCommand {
    type Result = A
    def nextState(s: State) = s
    def preCondition(s: State) = true
    def postCondition(s: State, result: Result) = s === result
    def run(sut: Sut) = sut.read.unsafePerformSync
  }

  // TODO: Exception
  private[this] final case class Modify(f: A => Either[Throwable, (A, B)]) extends Command {
    type Result = B
    def nextState(s: State) = f(s).fold(_ => s, _._1)
    def preCondition(s: State) = true
    def postCondition(s: State, r: Try[Result]) =
      (f(s), r) match {
        case (Right((_, expected)), Success(actual)) => expected === actual
        case (Left(expected), Failure(actual)) => expected == actual
        case _ => false
      }
    def run(sut: Sut) = sut.modify(a => f(a).fold(Task.fail, Task.now)).unsafePerformSync
  }
}

object TMVarSpec extends Specification with ScalaCheck {
  "TMVars" should {
    "allow the value to be read after put" in prop { a: Int =>
      (for {
        sut <- TMVar.newEmptyTMVar[Int]
        _ <- sut.put(a)
        result <- sut.read
      } yield result).unsafePerformSync ==== a
    }

    "allow the value to be modified" in prop { (a: Int, f: Int => (Int, Int)) =>
      (for {
        sut <- TMVar.newEmptyTMVar[Int]
        _ <- sut.put(a)
        result <- sut.modify(i => Task.now(f(i)))
        modified <- sut.read
      } yield (modified, result)).unsafePerformSync ==== f(a)
    }

    "preserve the old value on exception" in prop { (a: Int, t: Throwable) =>
      (for {
        sut <- TMVar.newEmptyTMVar[Int]
        _ <- sut.put(a)
        failure <- sut.modify(_ => Task.fail(t)).attempt
        result <- sut.read
      } yield (failure.swap.toOption, result)).unsafePerformSync ==== (t.some -> a)
    }

    "behave concurrently" in new TMVarCommands[Int, Int].property(threadCount = 4)
  }
}
