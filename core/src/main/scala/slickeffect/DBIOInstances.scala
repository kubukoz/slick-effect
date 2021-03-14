package slickeffect

import scala.util.Failure
import scala.util.Success
import slick.dbio.DBIO
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import cats.~>
import cats.effect.kernel.Sync
import cats.effect.std.Dispatcher
import cats.effect.kernel.CancelScope
import cats.effect.kernel.Poll

private[slickeffect] final class DBIOSync(implicit ec: ExecutionContext)
  extends Sync[DBIO] {

  def rootCancelScope: CancelScope = CancelScope.Uncancelable

  def forceR[A, B](fa: DBIO[A])(fb: DBIO[B]): DBIO[B] =
    productR(attempt(fa))(fb)

  def uncancelable[A](body: Poll[DBIO] => DBIO[A]): DBIO[A] =
    body(new Poll[DBIO] { def apply[X](fa: DBIO[X]): DBIO[X] = fa })

  def canceled: DBIO[Unit] = unit

  // There is no cancel.
  def onCancel[A](fa: DBIO[A], fin: DBIO[Unit]): DBIO[A] = fa

  override val realTime: slick.dbio.DBIO[FiniteDuration] = delay(
    System.currentTimeMillis().millis
  )

  override val monotonic: slick.dbio.DBIO[FiniteDuration] = delay(
    System.nanoTime().nanos
  )

  override def suspend[A](hint: Sync.Type)(thunk: => A): slick.dbio.DBIO[A] =
    // ignoring hint because there isn't much DBIO can do otherwise
    DBIO.successful(()).map(_ => thunk)

  override def flatMap[A, B](fa: DBIO[A])(f: A => DBIO[B]): DBIO[B] =
    fa.flatMap(f)

  override def tailRecM[A, B](a: A)(f: A => DBIO[Either[A, B]]): DBIO[B] =
    f(a).flatMap {
      case Left(a2) => tailRecM(a2)(f)
      case Right(b) => pure(b)
    }

  override def raiseError[A](e: Throwable): DBIO[A] = DBIO.failed(e)

  override def handleErrorWith[A](
    fa: DBIO[A]
  )(
    f: Throwable => DBIO[A]
  ): DBIO[A] =
    fa.asTry.flatMap {
      case Success(value) => pure(value)
      case Failure(e)     => f(e)
    }

  override def pure[A](x: A): DBIO[A] = DBIO.successful(x)
}

private[slickeffect] final class LiftEffectToDBIO[F[_]](
  dispatcher: Dispatcher[F],
  compute: ExecutionContext
) extends (F ~> DBIO) {

  def apply[A](fa: F[A]): slick.dbio.DBIO[A] =
    DBIO
      .successful(())
      .flatMap(_ => DBIO.from(dispatcher.unsafeToFuture(fa)))(compute)

}
