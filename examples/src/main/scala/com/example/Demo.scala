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

package com.example

import scala.concurrent.ExecutionContext

import cats.FlatMap
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.LiftIO
import cats.effect.kernel.Async
import cats.effect.std.Console
import cats.effect.unsafe.IORuntime
import cats.syntax.all._
import cats.tagless.implicits._
import cats.~>
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.PostgresProfile.api._
import slickeffect.Transactor
import slickeffect.catsio.implicits._
import slickeffect.implicits._
import cats.tagless.FunctorK

object Demo extends IOApp.Simple {
  implicit val rt: IORuntime = runtime
  implicit val ec: ExecutionContext = runtime.compute

  val run: IO[Unit] = {

    implicit val liftIODBIO: IO ~> DBIO = LiftIO.liftK[DBIO]

    val resource = for {
      transactor <- Transactor.fromDatabase[IO](
                      IO(
                        Database.forURL(
                          "jdbc:postgresql://localhost:5432/postgres",
                          "postgres",
                          "postgres"
                        )
                      )
                    )
    } yield {

      implicit val xa: Transactor[IO] =
        transactor.configure(slickeffect.transactor.config.transactionally)

      new Application[IO]
    }

    resource.use(_.run)
  }

}

class Application[F[_]: Async: Transactor](
)(
  implicit ec: ExecutionContext,
  fToDBIO: F ~> DBIO
) {
  private val fClient = AsyncClient.instance[F]
  private implicit val dbioClient: AsyncClient[DBIO] = fClient.mapK(fToDBIO)
  private implicit val repo: Repository[DBIO] = Repository.instance
  private implicit val console: Console[DBIO] =
    cats.effect.std.Console.make[F].mapK(fToDBIO)

  private val program = Program.instance[DBIO].mapK(Transactor[F].transactK)

  def run = program.run
}

trait Program[F[_]] {
  def run: F[Unit]
}

object Program {

  implicit val functorK: FunctorK[Program] = new FunctorK[Program] {

    def mapK[F[_], G[_]](af: Program[F])(fk: F ~> G): Program[G] =
      new Program[G] {
        def run: G[Unit] = fk(af.run)
      }

  }

  def instance[F[_]: AsyncClient: Repository: cats.effect.std.Console: FlatMap]
    : Program[F] =
    new Program[F] {

      val run: F[Unit] =
        Repository[F]
          .findAll
          .flatMap(cats.effect.std.Console[F].println(_)) *>
          AsyncClient[F].execute

    }

}

trait Repository[F[_]] {
  def findAll: F[List[String]]
}

object Repository {

  def apply[F[_]](
    implicit ev: Repository[F]
  ): Repository[F] = ev

  def instance(
    implicit ec: ExecutionContext
  ): Repository[DBIO] =
    new Repository[DBIO] {
      val findAll: DBIO[List[String]] =
        sql"select 'a'".as[String].map(_.toList)
    }

}

trait AsyncClient[F[_]] {
  def execute: F[Unit]
}

object AsyncClient {

  def apply[F[_]](
    implicit ev: AsyncClient[F]
  ): AsyncClient[F] = ev

  implicit val functorK: FunctorK[AsyncClient] = new FunctorK[AsyncClient] {

    def mapK[F[_], G[_]](af: AsyncClient[F])(fk: F ~> G): AsyncClient[G] =
      new AsyncClient[G] {
        def execute: G[Unit] = fk(af.execute)
      }

  }

  def instance[F[_]: Async]: AsyncClient[F] =
    new AsyncClient[F] {

      val execute: F[Unit] =
        Async[F].async_ { cb =>
          cb(Right(()))
        }

    }

}
