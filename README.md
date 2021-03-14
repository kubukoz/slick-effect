# slick-effect

[![License](https://img.shields.io/:license-Apache%202-green.svg)](https://www.apache.org/licenses/LICENSE-2.0.txt)

Compatibility tools for Slick + cats-effect. Released for Scala 2.12 and 2.13.

## Usage

Add the dependency. SBT:

```sbt
"com.kubukoz" %% "slick-effect" % "0.3.0",
// for the LiftIO instance
"com.kubukoz" %% "slick-effect-catsio" % "0.3.0"
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

Cats Effect 3 changes the hierarchy of the type classes that are available, in a way that only lets us implement `Sync` and `LiftIO`. This means we're losing the old `Async`.

To work around that limitation and ensure you can still combine `Async` with `DBIO` _somehow_, you can do one of these two things:

- Given an `Async[F]` for some `F[_]` (like `IO`), use `slickeffect.liftEffectToDBIO` to get a `Resource[F, F ~> DBIO]`, which you can `.use` and pass the value inside it whenever you need a conversion
- Use `LiftIO[DBIO]` from the new `catsio` module, which serves as a `IO ~> DBIO` without a resource. This one requires an `IORuntime`, a non-implicit one is provided inside `IOApp`, so you can pass that when defining this instance.

These should be useful to anyone who was using `Async[DBIO]` previously - for example, if you were instantiating a Tagless Final algebra that required `Async`:

```scala
trait Client[F[_]] {
  def call: F[Unit]
}

def asyncClient[F[_]: Async]: Client[F] = ...
```

**It is now impossible to directly get a `Client[DBIO]`, because of the `Async` constraint**. However, it is still possible to get to that instance through `IO` or any other type that you have a `~> DBIO` for:

```scala
val ioClient: Client[IO] = asyncClient[IO]

implicit val clientFunctorK: FunctorK[Client] = Derive.functorK

val dbioClient: Client[DBIO] = ioClient.mapK(slickeffect.liftToDBIO[IO])
```

`FunctorK` and `Derive` come from [cats-tagless](https://github.com/typelevel/cats-tagless) (`Derive.functorK` is a macro from the `macros` module).
A manually written instance of `FunctorK` would also work (these are usually trivial to write). For more complicated interfaces,
such as ones that have methods taking `F[_]`-shaped arguments, check out other type classes in cats-tagless (`InvariantK`, `ContravariantK`, etc.).

For a full usage of this pattern, check [the examples](examples/src/main/scala/com/example/Demo.scala).

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
