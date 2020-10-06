package slickeffect

import cats.effect.Sync
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext

object implicits extends DBIOInstances

trait DBIOInstances {
  implicit def dbioCatsEffectSync(implicit ec: ExecutionContext): Sync[DBIO] = new DBIOSync()
}
