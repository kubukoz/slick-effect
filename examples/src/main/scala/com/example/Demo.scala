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
import cats.tagless.autoFunctorK
import cats.tagless.finalAlg
import cats.tagless.implicits._
import cats.~>
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.PostgresProfile.api._
import slickeffect.Transactor
import slickeffect.catsio.implicits._
import slickeffect.implicits._

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

      implicit val xa =
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
  private implicit val console: Console[DBIO] = cats.effect.std.Console.make[DBIO]

  private val program = Program.instance[DBIO].mapK(Transactor[F].transactK)

  def run = program.run
}

@autoFunctorK
trait Program[F[_]] {
  def run: F[Unit]
}

object Program {

  def instance[
    F[_]: AsyncClient: Repository: cats.effect.std.Console: FlatMap
  ]: Program[F] =
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
