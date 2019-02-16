package slickeffect

import cats.effect._
import cats.effect.implicits._
import cats.~>
import slick.basic.{BasicBackend, BasicProfile, DatabaseConfig}
import slick.dbio.DBIO
import cats.implicits._

trait Transactor[F[_]] {
  def transact[A](dbio: DBIO[A]): F[A] = transactK(dbio)
  def transactK: DBIO ~> F

  def configure(f: DBIO ~> DBIO): Transactor[F] =
    Transactor.liftK(f andThen transactK)
}

object Transactor {
  def apply[F[_]](implicit F: Transactor[F]): Transactor[F] = F

  /**
    * Creates a Transactor from a factory of databases.
    * This transactor doesn't actually run the DBIO actions in transactions by default.
    * If you want transactions, see [[slickeffect.config.transactionally]] and its Scaladoc.
    */
  def fromDatabase[F[_]: Async: ContextShift](
    dbF: F[BasicBackend#DatabaseDef]
  ): Resource[F, Transactor[F]] =
    Resource.make(dbF)(db => IO.fromFuture(IO(db.shutdown)).to[F]).map { db =>
      liftK {
        λ[DBIO ~> F] { dbio =>
          IO.fromFuture(IO(db.run(dbio))).to[F].guarantee(ContextShift[F].shift)
        }
      }
    }

  def fromDatabaseConfig[F[_]: Async: ContextShift](
    dbConfig: DatabaseConfig[_ <: BasicProfile]
  ): Resource[F, Transactor[F]] =
    fromDatabase(Sync[F].delay(dbConfig.db))

  def liftK[F[_]](f: DBIO ~> F): Transactor[F] = new Transactor[F] {
    override val transactK: DBIO ~> F = f
  }
}
