package slickeffect

import cats.effect.IO
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import slick.dbio.DBIO
import slick.jdbc.H2Profile
import slick.jdbc.JdbcProfile
import slickeffect.transactor.config
import cats.effect.unsafe.IORuntime

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
    }.unsafeToFuture()(IORuntime.global)
  }
}
