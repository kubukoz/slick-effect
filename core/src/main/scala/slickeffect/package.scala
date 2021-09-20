import slick.dbio.DBIO

import cats.~>
import scala.concurrent.ExecutionContext
import cats.effect.kernel.Async
import cats.effect.std.Dispatcher
import cats.effect.kernel.Resource
import cats.effect.kernel.syntax.all._
import cats.syntax.all._

package object slickeffect {

  def liftEffectToDBIO[F[_]: Async]: Resource[F, F ~> DBIO] =
    (Dispatcher[F], Async[F].executionContext.toResource)
      .mapN(liftEffectUsingDispatcher)

  def liftEffectUsingDispatcher[F[_]](
    dispatcher: Dispatcher[F],
    compute: ExecutionContext
  ): F ~> DBIO = new LiftEffectToDBIO[F](dispatcher, compute)

}
