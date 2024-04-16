/*
 * Copyright 2019 Jakub Kozłowski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
