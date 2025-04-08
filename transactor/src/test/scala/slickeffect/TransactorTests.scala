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

import cats.effect.IO
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import slick.dbio.DBIO
import slick.jdbc.H2Profile
import slick.jdbc.JdbcProfile
import slickeffect.transactor.config

import cats.effect.unsafe.implicits.*

class TransactorTests extends AsyncWordSpec with Matchers {

  "fromDatabase" should {
    "make it possible to run actions" in {

      implicit val profile: JdbcProfile = H2Profile

      val action: DBIO[Int] = {
        import profile.api._
        sql"select 1".as[Int].head
      }

      Transactor
        .fromDatabase[IO](IO(profile.backend.Database.forURL("jdbc:h2:mem:")))
        .map(_.configure(config.transactionally))
        .use(_.transact(action))
        .map(_ shouldBe 1)
    }.unsafeToFuture()
  }
}
