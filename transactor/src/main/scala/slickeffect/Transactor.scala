package slickeffect

import cats.effect._
import cats.~>
import slick.basic.{BasicBackend, BasicProfile, DatabaseConfig}
import slick.dbio.DBIO
import slick.dbio.NoStream
import slick.dbio.Effect
import slick.dbio.DBIOAction

trait Transactor[F[_], -S <: NoStream, +E <: Effect] { self =>

  def transactAction[A](dbio: DBIOAction[A, S, E]): F[A]

  //todo move these two somewhere else?
  def transact[A](dbio: DBIO[A])(implicit ev: self.type <:< Transactor[F, NoStream, Effect.All]): F[A] =
    ev(self).transactAction(dbio)

  def transactK(implicit ev: self.type <:< Transactor[F, NoStream, Effect.All]): DBIO ~> F =
    λ[DBIO ~> F](transact(_))

  def configure[NewS <: NoStream, NewE <: Effect](
    f: DBIOFunctionK[NewS, NewE, S, E]
  ): Transactor[F, NewS, NewE] = {
    new Transactor[F, NewS, NewE] {
      def transactAction[A](dbio: DBIOAction[A, NewS, NewE]): F[A] =
        self.transactAction(f(dbio))
    }
  }

}

object Transactor {
  type DBIOOnly[F[_]] = Transactor[F, NoStream, Effect.All]
  type Gen[F[_]]      = Transactor[F, NoStream, Effect]

  /**
    * Creates a Transactor from a factory of databases.
    * This transactor doesn't actually run the DBIO actions in transactions by default.
    * If you want transactions, see [[slickeffect.transactor.config.transactionally]] and its Scaladoc.
    */
  def fromDatabase[F[_]: Async](
    dbF: F[BasicBackend#DatabaseDef]
  ): Resource[F, Transactor[F, NoStream, Effect.All]] =
    Resource.make(dbF)(db => Async[F].fromFuture(Sync[F].delay(db.shutdown))).map { db =>
      new Transactor[F, NoStream, Effect.All] {
        def transactAction[A](dbio: DBIOAction[A, NoStream, Effect.All]): F[A] =
          Async[F].fromFuture(Sync[F].delay(db.run(dbio)))
      }
    }

  def fromDatabaseConfig[F[_]: Async](
    dbConfig: DatabaseConfig[_ <: BasicProfile]
  ): Resource[F, Transactor[F, NoStream, Effect]] =
    fromDatabase(Sync[F].delay(dbConfig.db))

  def liftK[F[_]](f: DBIO ~> F): Transactor.DBIOOnly[F] = new Transactor[F, NoStream, Effect.All] {
    def transactAction[A](dbio: DBIOAction[A, NoStream, Effect.All]): F[A] = f(dbio)
  }

  // type Aux[F[_], S_ <: NoStream, E_ >: Effect.All <: Effect] = Transactor[F] {
  //   type S = S_
  //   type E = E_
  // }

  // type DBIOOnly[F[_]] = Transactor.Aux[F, NoStream, Effect.All]

  // def apply[F[_]](implicit F: Transactor[F]): F.type = F
}

/**
  * Universally qualified function on DBIOs, capable of changing the stream and effect types - keeping the result type.
  */
trait DBIOFunctionK[-S1 <: NoStream, +E1 <: Effect, +S2 <: NoStream, -E2 <: Effect] {
  def apply[A](dbio: DBIOAction[A, S1, E1]): DBIOAction[A, S2, E2]
}
