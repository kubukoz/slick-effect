package slickeffect.catsio

import cats.effect.LiftIO
import cats.effect.unsafe.IORuntime
import slick.dbio.DBIO
import cats.effect.IO

object implicits {

  implicit def dbioCatsEffectLiftIO(implicit runtime: IORuntime): LiftIO[DBIO] =
    new DBIOLiftIO()
}

private[catsio] final class DBIOLiftIO(implicit runtime: IORuntime)
  extends LiftIO[DBIO] {

  def liftIO[A](ioa: IO[A]): DBIO[A] =
    DBIO
      .successful(())
      .flatMap(_ => DBIO.from(ioa.unsafeToFuture()))(runtime.compute)

}
