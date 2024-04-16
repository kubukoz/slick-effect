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
