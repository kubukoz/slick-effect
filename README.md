# slick-effect

[![License](http://img.shields.io/:license-Apache%202-green.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)

Compatibility tools for Slick + cats-effect. Released for Scala 2.12 and 2.13.

## Usage

Add the dependency. SBT:

```sbt
"com.kubukoz" %% "slick-effect" % "0.3.0"
```

Ammonite:

```
$ivy.`com.kubukoz::slick-effect:0.3.0`
```

Coursier:

```
com.kubukoz::slick-effect:0.3.0
```

### Instances

Import the instances:

```scala
import slickeffect.implicits._

//an implicit EC is needed for cpu-bound work on DBIOs (map, flatMap)
import scala.concurrent.ExecutionContext.Implicits.global

//the instances will be in implicit scope
scala> Sync[slick.dbio.DBIO]
res0: Sync[slick.dbio.package.DBIO] = slickeffect.DBIOSync@434c179e
```

### Cats Effect 1.x/2.x -> 3.x migration guide

Cats Effect 3 changes the hierarchy of the type classes that are available, in a way that only lets us implement `Sync`.

To work around that limitation and ensure you can still combine `Async` with `DBIO` _somehow_, a new method has been added: `liftToDBIO`.
It returns an `F ~> DBIO` for any `UnsafeRun`nable `F[_]` (such as `IO`, given a runtime instance, which `IOApp` will happily deliver to you).
This should be useful to anyone who was using `Async[DBIO]` previously - for example, if you were instantiating a Tagless Final algebra that required `Async`:

```scala
trait Client[F[_]] {
  def call: F[Unit]
}

def asyncClient[F[_]: Async]: Client[F] = ...
```

It is now impossible to directly get a `Client[DBIO]`, because of the `Async` constraint. However, it is still possible to get there through e.g. `IO`:

```scala
val ioClient: Client[IO] = asyncClient[IO]

implicit val clientFunctorK: FunctorK[Client] = Derive.functorK

val dbioClient: Client[DBIO] = ioClient.mapK(slickeffect.liftToDBIO[IO])
```

`FunctorK` and `Derive` come from [cats-tagless](https://github.com/typelevel/cats-tagless) (`Derive.functorK` is a macro from the `macros` module).
A manually written instance of `FunctorK` would also work, of course. For more complicated interfaces, such as ones that have
methods taking `F[_]`-shaped arguments, check out other type classes in cats-tagless (`InvariantK`, `ContravariantK`, etc.).

### [EXPERIMENTAL] Transactor (from 0.3.0-M2 onwards)

You can use slick-effect to run your DBIOs. This functionality is experimental, and the API may change.

If you still want to use it, add a dependency on the transactor module:

```scala
"com.kubukoz" %% "slick-effect-transactor" % "0.3.0"
```

Create a transactor:

```scala
val transactorResource: Resource[IO, Transactor[IO]]
  .fromDatabase[IO](IO(Database.forURL("jdbc:h2:mem:"))) //or .fromDatabaseConfig
  .map(_.configure(config.transactionally)) //or any DBIO ~> DBIO
  .use(_.transact(action))


val result: DBIO[Int] = ???

transactorResource.use { tx =>
  tx.transact(result): IO[Int]
}
```
