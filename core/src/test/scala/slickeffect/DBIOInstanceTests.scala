package slickeffect

import cats.data.EitherT
import cats.kernel.Eq
import cats.laws.discipline.SemigroupalTests
import cats.laws.discipline.SemigroupalTests.Isomorphisms
import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import slick.dbio.DBIO
import slick.jdbc.H2Profile

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Try
import cats.tests.CatsSuite
import cats.effect.laws.SyncTests
import cats.effect.testkit.TestInstances
import org.scalacheck.Prop

class DBIOInstanceTests extends CatsSuite with TestInstances {
  import slickeffect.implicits._

  private val timeout = 3.seconds
  val db = H2Profile.backend.Database.forURL("jdbc:h2:mem:")

  implicit val throwableEq: Eq[Throwable] = Eq.by(_.getMessage)

  implicit val dbioIsomorphisms: Isomorphisms[DBIO] =
    SemigroupalTests.Isomorphisms.invariant[DBIO]

  implicit val throwableArbitrary: Arbitrary[Throwable] =
    Arbitrary(Arbitrary.arbitrary[Exception].map(new Exception(_)))

  implicit def dbioArbitrary[T: Arbitrary]: Arbitrary[DBIO[T]] =
    Arbitrary(
      Gen.oneOf(
        Arbitrary.arbitrary[T].map(DBIO.successful),
        Arbitrary.arbitrary[Throwable].map(DBIO.failed)
      )
    )

  implicit def dbioEq[A: Eq]: Eq[DBIO[A]] =
    new Eq[DBIO[A]] {

      def eqv(fx: DBIO[A], fy: DBIO[A]): Boolean =
        Either.fromTry(Try(Await.result(db.run(fx), timeout))) === Either
          .fromTry(
            Try(Await.result(db.run(fy), timeout))
          )

    }

  implicit val dbioRun: DBIO[Boolean] => Prop = dbio =>
    Await.result(db.run(dbio.map(Prop.propBoolean)), timeout)

  implicit def eithertDBIOEq[E: Eq, T: Eq]: Eq[EitherT[DBIO, E, T]] =
    Eq.by(_.value)

  checkAll("Sync[DBIO]", SyncTests[DBIO].sync[Int, Int, Int])
}
