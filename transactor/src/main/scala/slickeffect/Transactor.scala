package slickeffect

import cats.effect._
import cats.~>
import slick.basic.{BasicBackend, BasicProfile, DatabaseConfig}
import slick.dbio.DBIO
import slick.dbio.NoStream
import slick.dbio.Effect
import slick.dbio.DBIOAction
import slick.jdbc.JdbcProfile
import slickeffect.transactor.config
import slick.dbio.Streaming

trait TransactorCov[F[_], +S <: NoStream, -E <: Effect] { self =>

  def transactAction[A, S2 >: S <: NoStream, E2 <: E](dbio: DBIOAction[A, S2, E2]): F[A]

  // def transact[A](dbio: DBIO[A]): F[A] = ??? // transactAction(??? : DBIOAction[A, S, Effect.Transactional])
  // def transactK: DBIO ~> F             = λ[DBIO ~> F](transact(_))

  def foo(arg: TransactorCov[F, NoStream, Effect]) = {

    //widen/narrow
    val w = arg: TransactorCov[F, NoStream, Effect.All]

    val x: DBIOAction[Int, NoStream, Effect.Read] = ???

    w.transactAction(x)
    arg.transactAction(x)

    ???
  }

  def configureK[NewS <: NoStream, NewE <: Effect, S0 >: S <: NoStream, E0 <: E](
    f: Any //DBIOFunctionK[NewS, NewE, S0, E0]
  ): TransactorCov[F, NewS, NewE] = {
    new TransactorCov[F, NewS, NewE] {
      def transactAction[A, S2 >: NewS <: NoStream, E2 <: NewE](dbio: DBIOAction[A, S2, E2]): F[A] = {
        val x: E    = ???
        val y: NewE = ???
        val z: E2   = ???

        val a: DBIOAction[A, _ >: S <: NoStream, _ <: E] = dbio

        self.transactAction(dbio)
        // self.transactAction(f(dbio))
        ???
      }
    }
  }

}

object TransactorCov {
  type Gen[F[_]] = TransactorCov[F, NoStream, Effect.All]

  def fromDatabase[F[_]: Async](
    dbF: F[BasicBackend#DatabaseDef]
  ): Resource[F, TransactorCov[F, NoStream, Effect]] =
    Resource.make(dbF)(db => Async[F].fromFuture(Sync[F].delay(db.shutdown))).map { db =>
      new TransactorCov[F, NoStream, Effect] {
        def transactAction[A, S2 >: NoStream <: NoStream, E2 <: Effect](dbio: DBIOAction[A, S2, E2]): F[A] =
          Async[F].fromFuture(Sync[F].delay(db.run(dbio)))
      }
    }
}

trait Transactor[F[_]] { self =>
  type S <: NoStream

  type E >: Effect.All <: Effect

  def transactAction[A, S2 >: S <: NoStream, E2 >: Effect.All <: E](dbio: DBIOAction[A, S2, E2]): F[A]

  def transact[A](dbio: DBIO[A]): F[A] = transactAction(dbio)
  def transactK: DBIO ~> F             = λ[DBIO ~> F](transact(_))

  //todo: this might have to go
  def configure(f: DBIO ~> DBIO): Transactor.DBIOOnly[F] =
    Transactor.liftK(f andThen transactK)

  def configureK[S2 <: NoStream, E2 >: Effect.All <: Effect](
    f: DBIOFunctionK[S2, E2, S, E]
  ): Transactor.Aux[F, S2, E2] = {
    type S_ = S2
    type E_ = E2

    new Transactor[F] {
      type S = S_
      type E = E_

      def transactAction[A, S3 >: S2 <: NoStream, E3 >: Effect.All <: E2](dbio: DBIOAction[A, S3, E3]): F[A] = {
        val g: DBIOFunctionK[S3, E3, self.S, self.E]  = ???
        val g0: DBIOFunctionK[S2, E2, self.S, self.E] = g

        self.transactAction {
          dbio
          // g(dbio)
        }
      }
    }
  }

  ???
  // Transactor.liftK(f andThen transactK)
}

object Transactor {

  //this is just an example of variance, to be removed
  implicit class TransactorTransactionalOps[F[_], S <: NoStream, E >: Effect.All <: Effect with Effect.Transactional](
    xa: Transactor.Aux[F, S, E]
  ) {

    def transactional(implicit jdbc: JdbcProfile): Transactor.Aux[F, S, E] = {
      val fk = config.transactionallyK[S, E with Effect.Transactional]

      xa.configureK(fk)
    }
  }

  type Aux[F[_], S_ <: NoStream, E_ >: Effect.All <: Effect] = Transactor[F] {
    type S = S_
    type E = E_
  }

  type DBIOOnly[F[_]] = Transactor.Aux[F, NoStream, Effect.All]

  def apply[F[_]](implicit F: Transactor[F]): F.type = F

  /**
    * Creates a Transactor from a factory of databases.
    * This transactor doesn't actually run the DBIO actions in transactions by default.
    * If you want transactions, see [[slickeffect.transactor.config.transactionally]] and its Scaladoc.
    */
  def fromDatabase[F[_]: Async](
    dbF: F[BasicBackend#DatabaseDef]
  ): Resource[F, Transactor.DBIOOnly[F]] =
    Resource.make(dbF)(db => Async[F].fromFuture(Sync[F].delay(db.shutdown))).map { db =>
      liftK {
        λ[DBIO ~> F] { dbio =>
          Async[F].fromFuture(Sync[F].delay(db.run(dbio)))
        }
      }
    }

  def fromDatabaseConfig[F[_]: Async](
    dbConfig: DatabaseConfig[_ <: BasicProfile]
  ): Resource[F, Transactor.DBIOOnly[F]] =
    fromDatabase(Sync[F].delay(dbConfig.db))

  def liftK[F[_]](f: DBIO ~> F): Transactor.DBIOOnly[F] = new Transactor[F] {

    type S = NoStream
    type E = Effect.All

    def transactAction[A, S2 >: NoStream <: NoStream, E2 >: Effect.All <: Effect.All](
      dbio: DBIOAction[A, S2, E2]
    ): F[A] = f(dbio)
  }
}

/**
  * Universally qualified function on DBIOs, capable of changing the stream and effect types - keeping the result type.
  */
trait DBIOFunctionK[-S1 <: NoStream, +E1 <: Effect, +S2 <: NoStream, -E2 <: Effect] {
  def apply[A](dbio: DBIOAction[A, S1, E1]): DBIOAction[A, S2, E2]
}
