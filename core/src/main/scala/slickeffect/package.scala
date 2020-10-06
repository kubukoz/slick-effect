import cats.effect.unsafe.UnsafeRun
import slick.dbio.DBIO
import cats.~>
import scala.concurrent.ExecutionContext

package object slickeffect {

  def liftToDBIO[F[_]: UnsafeRun](implicit ec: ExecutionContext): F ~> DBIO = new (F ~> DBIO) {

    def apply[A](fa: F[A]): DBIO[A] =
      DBIO.successful(()).flatMap(_ => DBIO.from(UnsafeRun[F].unsafeRunFutureCancelable(fa)._1))
  }
}
