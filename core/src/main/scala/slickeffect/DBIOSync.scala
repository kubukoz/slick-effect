package slickeffect

import slick.dbio.DBIO

import scala.util.{Failure, Success}
import cats.effect.kernel.Sync
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

private[slickeffect] class DBIOSync(implicit ec: ExecutionContext) extends Sync[DBIO] {

  override val realTime: slick.dbio.DBIO[FiniteDuration]  = delay(System.currentTimeMillis().millis)
  override val monotonic: slick.dbio.DBIO[FiniteDuration] = delay(System.nanoTime().nanos)

  override def suspend[A](hint: Sync.Type)(thunk: => A): slick.dbio.DBIO[A] = {
    // ignoring hint because there isn't much DBIO can do otherwise
    DBIO.successful(()).map(_ => thunk)
  }

  override def flatMap[A, B](fa: DBIO[A])(f: A => DBIO[B]): DBIO[B] = fa.flatMap(f)

  override def tailRecM[A, B](a: A)(f: A => DBIO[Either[A, B]]): DBIO[B] = f(a).flatMap {
    case Left(a2) => tailRecM(a2)(f)
    case Right(b) => pure(b)
  }

  override def raiseError[A](e: Throwable): DBIO[A] = DBIO.failed(e)
  override def handleErrorWith[A](fa: DBIO[A])(f: Throwable => DBIO[A]): DBIO[A] = fa.asTry.flatMap {
    case Success(value) => pure(value)
    case Failure(e)     => f(e)
  }

  override def pure[A](x: A): DBIO[A] = DBIO.successful(x)
}
