package slickeffect

import cats.MonadError
import cats.effect.{Async, ExitCase}
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Promise}
import scala.util.{Failure, Success}

private[slickeffect] class DBIOAsync(implicit ec: ExecutionContext) extends Async[DBIO] {
  private implicit val monadErrorInstance: MonadError[DBIO, Throwable] = this

  override def async[A](
    k: (Either[Throwable, A] => Unit) => Unit
  ): DBIO[A] = suspend {
    val promise = Promise[A]()

    k {
      case Right(a) => promise.success(a)
      case Left(t)  => promise.failure(t)
    }

    DBIO.from(promise.future)
  }

  override def asyncF[A](
    k: (Either[Throwable, A] => Unit) => DBIO[Unit]
  ): DBIO[A] = suspend {
    val promise = Promise[A]()

    k {
      case Right(a) => promise.success(a)
      case Left(t)  => promise.failure(t)
    } >> DBIO.from(promise.future)
  }

  override def bracketCase[A, B](acquire: DBIO[A])(
    use: A => DBIO[B]
  )(release: (A, ExitCase[Throwable]) => DBIO[Unit]): DBIO[B] = {
    import cats.syntax.all._

    acquire.flatMap { a =>
      use(a).attempt.flatTap {
        case Right(_) => release(a, ExitCase.complete)
        case Left(e)  => release(a, ExitCase.error(e))
      }.rethrow
    }
  }

  override def suspend[A](thunk: => DBIO[A]): DBIO[A] =
    DBIO.successful(()).flatMap(_ => thunk)

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
