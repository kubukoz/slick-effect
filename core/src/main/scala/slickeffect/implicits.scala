package slickeffect

import slick.dbio.DBIO
import scala.concurrent.ExecutionContext
import cats.effect.kernel.Sync

object implicits extends DBIOInstances

trait DBIOInstances {
  implicit def dbioCatsEffectSync(implicit ec: ExecutionContext): Sync[DBIO] =
    new DBIOSync()
}
