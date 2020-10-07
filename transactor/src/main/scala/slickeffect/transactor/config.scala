package slickeffect.transactor

import cats.~>
import slick.dbio.DBIO
import slick.jdbc.JdbcProfile
import slickeffect.DBIOFunctionK
import slick.dbio.NoStream
import slick.dbio.Effect
import slick.dbio.DBIOAction

object config {

  /**
    * Usage:
    *
    * import slickeffect.config._
    * def xa: SlickTransactor[F]
    *
    * def withTransactions: SlickTransactor[F] = xa.configure(transactionally)
    * */
  def transactionally(implicit profile: JdbcProfile): DBIO ~> DBIO = {
    λ[DBIO ~> DBIO](transactionallyK(profile)(_))
  }

  def transactionallyK[S <: NoStream, E <: Effect](
    implicit profile: JdbcProfile
  ): DBIOFunctionK[S, E, S, E with Effect.Transactional] = new DBIOFunctionK[S, E, S, E with Effect.Transactional] {

    def apply[A](dbio: DBIOAction[A, S, E]): DBIOAction[A, S, E with Effect.Transactional] = {
      import profile.api.jdbcActionExtensionMethods

      dbio.transactionally
    }
  }
}
