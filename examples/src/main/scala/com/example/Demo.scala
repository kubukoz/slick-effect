package com.example

import cats.effect.kernel.Async
import cats.effect.IOApp
import cats.effect.IO
import slickeffect.implicits._
import slickeffect.catsio.implicits._
import cats.tagless.implicits._
import cats.tagless.autoFunctorK
import slick.dbio.DBIO
import slickeffect.Transactor
import cats.tagless.finalAlg
import slick.jdbc.JdbcBackend.Database
import cats.~>
import scala.concurrent.ExecutionContext
import cats.effect.kernel.Sync
import cats.syntax.all._
import cats.effect.LiftIO

object Demo extends IOApp.Simple {
  implicit val rt = runtime
  implicit val ec = runtime.compute

  val run: IO[Unit] = {

    implicit val liftIODBIO = LiftIO.liftK[DBIO]

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
      implicit val xa = transactor

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
  private implicit val dbioClient = fClient.mapK(fToDBIO)
  private implicit val repo = Repository.instance

  private val program = Program.instance[DBIO].mapK(Transactor[F].transactK)

  def run = program.run
}

@autoFunctorK
trait Program[F[_]] {
  def run: F[Unit]
}

object Program {

  def instance[F[_]: AsyncClient: Repository: Sync]: Program[F] =
    new Program[F] {

      val run: F[Unit] =
        Repository[F]
          .findAll
          .flatMap(cats.effect.std.Console[F].println(_)) *>
          AsyncClient[F].execute

    }

}

@finalAlg
trait Repository[F[_]] {
  def findAll: F[List[String]]
}

object Repository {

  def instance(implicit ec: ExecutionContext): Repository[DBIO] =
    new Repository[DBIO] {
      import slick.jdbc.PostgresProfile.api.{DBIO => _, _}

      val findAll: DBIO[List[String]] =
        sql"select 'a'".as[String].map(_.toList)
    }

}

@autoFunctorK
@finalAlg
trait AsyncClient[F[_]] {
  def execute: F[Unit]
}

object AsyncClient {

  def instance[F[_]: Async]: AsyncClient[F] =
    new AsyncClient[F] {

      val execute: F[Unit] =
        Async[F].async_ { cb =>
          cb(Right(()))
        }

    }

}
