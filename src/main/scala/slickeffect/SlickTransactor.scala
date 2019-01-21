package slickeffect

import cats.effect._
import cats.effect.implicits._
import cats.~>
import slick.basic.{BasicProfile, DatabaseConfig}
import slick.dbio.DBIO
import slick.jdbc.JdbcProfile
import cats.implicits._

trait SlickTransactor[F[_]] {
  def transact[A](dbio: DBIO[A]): F[A] = transactK(dbio)
  def transactK: DBIO ~> F

  def configure(f: DBIO ~> DBIO): SlickTransactor[F] = SlickTransactor.liftNT(f andThen transactK)
}

object SlickTransactor {

  def fromDatabase[F[_]: Async: ContextShift](
    dbF: F[BasicProfile#Backend#DatabaseDef]): Resource[F, SlickTransactor[F]] =
    Resource.make(dbF)(db => IO.fromFuture(IO(db.shutdown)).to[F]).map { db =>
      liftNT {
        λ[DBIO ~> F] { dbio =>
          IO.fromFuture(IO(db.run(dbio))).to[F].guarantee(ContextShift[F].shift)
        }
      }
    }

  def fromDatabaseConfig[F[_]: Async: ContextShift](
    dbConfig: DatabaseConfig[_ <: BasicProfile]): Resource[F, SlickTransactor[F]] =
    fromDatabase(Sync[F].delay(dbConfig.db))

  def liftNT[F[_]](f: DBIO ~> F): SlickTransactor[F] = new SlickTransactor[F] { override val transactK: DBIO ~> F = f }

  object configuration {

    def transactionally(implicit profile: JdbcProfile): DBIO ~> DBIO = {
      import profile.api.{DBIO => _, _}
      λ[DBIO ~> DBIO](_.transactionally)
    }
  }
}
