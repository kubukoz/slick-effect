package slickeffect

import cats.effect._
import cats.~>
import slick.basic.{BasicBackend, BasicProfile, DatabaseConfig}
import slick.dbio.DBIO

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
    * If you want transactions, see [[slickeffect.transactor.config.transactionally]] and its Scaladoc.
    */
  def fromDatabase[F[_]: Async: ContextShift](
    dbF: F[BasicBackend#DatabaseDef]
  ): Resource[F, Transactor[F]] =
    Resource.make(dbF)(db => Async.fromFuture(Sync[F].delay(db.shutdown))).map { db =>
      liftK {
        λ[DBIO ~> F] { dbio =>
          Async.fromFuture(Sync[F].delay(db.run(dbio)))
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
