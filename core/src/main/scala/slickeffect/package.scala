/*
 * Copyright 2019 Jakub Kozłowski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    (Dispatcher.parallel[F], Async[F].executionContext.toResource)
      .mapN(liftEffectUsingDispatcher)

  def liftEffectUsingDispatcher[F[_]](
    dispatcher: Dispatcher[F],
    compute: ExecutionContext
  ): F ~> DBIO = new LiftEffectToDBIO[F](dispatcher, compute)

}
