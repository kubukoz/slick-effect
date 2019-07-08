package slickeffect

import cats.effect.Async
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext

object implicits extends DBIOInstances

trait DBIOInstances {
  implicit def dbioCatsEffectAsync(implicit ec: ExecutionContext): Async[DBIO] = new DBIOAsync()
}
