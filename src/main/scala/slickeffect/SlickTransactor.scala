package slickeffect

import cats.effect._
import cats.effect.implicits._
import cats.implicits._
import cats.~>
import slick.basic.{BasicProfile, DatabaseConfig}
import slick.dbio.DBIO
import slick.jdbc.JdbcProfile

trait SlickTransactor[F[_]] {
  def transact[A](dbio: DBIO[A]): F[A]
  def transactK: DBIO ~> F = λ[DBIO ~> F](transact(_))

  def configure(f: DBIO ~> DBIO): SlickTransactor[F] = new ConfiguredSlickTransactor(this, f)
}

object SlickTransactor {

  def fromDatabase[F[_]: Async: ContextShift](
    dbF: F[BasicProfile#Backend#DatabaseDef]): Resource[F, SlickTransactor[F]] =
    Resource.make(dbF)(db => IO.fromFuture(IO(db.shutdown)).to[F]).map { db =>
      new SlickTransactor[F] {
        override def transact[A](dbio: DBIO[A]): F[A] =
          IO.fromFuture(IO(db.run(dbio))).to[F].guarantee(ContextShift[F].shift)
      }
    }

  def fromDatabaseConfig[F[_]: Async: ContextShift](
    dbConfig: DatabaseConfig[_ <: BasicProfile]): Resource[F, SlickTransactor[F]] =
    fromDatabase(Sync[F].delay(dbConfig.db))

  def transactionally(implicit profile: JdbcProfile): DBIO ~> DBIO = {
    import profile.api.{DBIO => _, _}
    λ[DBIO ~> DBIO](_.transactionally)
  }
}

class ConfiguredSlickTransactor[F[_]](underlying: SlickTransactor[F], f: DBIO ~> DBIO) extends SlickTransactor[F] {
  override def transact[A](dbio: DBIO[A]): F[A] = underlying.transact(f(dbio))
}
