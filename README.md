# slick-effect

[![License](http://img.shields.io/:license-Apache%202-green.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)

Compatibility tools for Slick + cats-effect. Released for Scala 2.12 and 2.11.

## Usage

Add the dependency. SBT:

```sbt
libraryDependencies += "com.kubukoz" %% "slick-effect" % "0.1.0"
```

Import the instances:

```scala
import slickeffect.implicits._

//an implicit EC is needed for cpu-bound work on DBIOs (map, flatMap)
import scala.concurrent.ExecutionContext.Implicits.global

//the instances will be in implicit scope
scala> Async[slick.dbio.DBIO]
res0: Async[slick.dbio.package.DBIO] = slickeffect.DBIOAsync@434c179e
```
