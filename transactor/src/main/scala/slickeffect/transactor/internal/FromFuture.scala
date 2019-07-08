package slickeffect.transactor.internal

import cats.ApplicativeError
import cats.effect.{Async, ContextShift}
import cats.implicits._
import cats.effect.implicits._

import scala.concurrent.{ExecutionContext, Future}

//might be replaced with polymorphic fromFuture in cats-effect
private[slickeffect] trait FromFuture[F[_]] {
  def fromFuture[A](ffa: F[Future[A]]): F[A]
}

object FromFuture {
  def apply[F[_]](implicit F: FromFuture[F]): FromFuture[F] = F

  implicit def instance[F[_]: Async: ContextShift]: FromFuture[F] = new FromFuture[F] {
    override def fromFuture[A](ffa: F[Future[A]]): F[A] = ffa.flatMap { future =>
      future.value match {
        case Some(value) =>
          ApplicativeError[F, Throwable].fromTry(value)
        case None =>
          Async[F]
            .async[A] { cb =>
              future.onComplete { tried =>
                cb(Either.fromTry(tried))
              }(immediate)
            }
            .guarantee(ContextShift[F].shift)
      }
    }
  }

  val immediate: ExecutionContext =
    new ExecutionContext {
      def execute(r: Runnable): Unit = r.run()

      def reportFailure(e: Throwable): Unit =
        Thread.getDefaultUncaughtExceptionHandler match {
          case null => e.printStackTrace()
          case h    => h.uncaughtException(Thread.currentThread(), e)
        }
    }
}
