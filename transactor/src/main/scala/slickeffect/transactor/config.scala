package slickeffect.transactor

import cats.~>
import slick.dbio.DBIO
import slick.jdbc.JdbcProfile

object config {

  /** Usage:
    *
    * import slickeffect.config._ def xa: SlickTransactor[F]
    *
    * def withTransactions: SlickTransactor[F] = xa.configure(transactionally)
    */
  def transactionally(implicit profile: JdbcProfile): DBIO ~> DBIO = {
    import profile.api.{DBIO => _, _}
    new (DBIO ~> DBIO) {
      def apply[A](fa: DBIO[A]): DBIO[A] = fa.transactionally
    }
  }

}
