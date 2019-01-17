package slickeffect

import cats.effect.{ContextShift, IO}
import org.scalatest.{AsyncWordSpec, Matchers}
import slick.dbio.DBIO
import slick.jdbc.{H2Profile, JdbcProfile}

import scala.concurrent.ExecutionContext

class SlickTransactorTests extends AsyncWordSpec with Matchers {
  "fromDatabase" should {
    "make it possible to run actions" in {
      implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

      implicit val profile: JdbcProfile = H2Profile

      val action: DBIO[Int] = {
        import profile.api._
        sql"select 1".as[Int].head
      }

      SlickTransactor
        .fromDatabase[IO](IO(profile.backend.Database.forURL("jdbc:h2:mem:")))
        .use(_.transact(action))
        .map(_ shouldBe 1)
    }.unsafeToFuture()
  }
}
