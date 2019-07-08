# slick-effect

[![License](http://img.shields.io/:license-Apache%202-green.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)

Compatibility tools for Slick + cats-effect. Released for Scala 2.12 and 2.11.

## Usage

Add the dependency. SBT:

```sbt
"com.kubukoz" %% "slick-effect" % "0.1.0"
```

Ammonite:

```
$ivy.`com.kubukoz::slick-effect:0.1.0`
```

Coursier:

```
com.kubukoz::slick-effect:0.1.0
```

### Instances

Import the instances:

```scala
import slickeffect.implicits._

//an implicit EC is needed for cpu-bound work on DBIOs (map, flatMap)
import scala.concurrent.ExecutionContext.Implicits.global

//the instances will be in implicit scope
scala> Async[slick.dbio.DBIO]
res0: Async[slick.dbio.package.DBIO] = slickeffect.DBIOAsync@434c179e
```

### Transactor (from 0.3.0-M2 onwards)

You can use slick-effect to run your DBIOs. Add a dependency on the transactor module:

```scala
"com.kubukoz" %% "slick-effect-transactor" % "0.3.0-M2"
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
