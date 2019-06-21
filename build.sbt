val Scala_2_11 = "2.11.12"
val Scala_2_12 = "2.12.8"
val Scala_2_13 = "2.13.0"

inThisBuild(
  List(
    organization := "com.kubukoz",
    homepage := Some(url("https://github.com/kubukoz/slick-effect")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
      Developer(
        "kubukoz",
        "Jakub Kozłowski",
        "kubukoz@gmail.com",
        url("https://kubukoz.com")
      )
    )
  ))

val compilerPlugins = List(
  compilerPlugin("org.spire-math" %% "kind-projector" % "0.9.10")
)

val commonSettings = Seq(
  scalaVersion := "2.11.11",
  Options.addAll,
  fork in Test := true,
  crossScalaVersions := Seq(Scala_2_11, Scala_2_12, Scala_2_13),
  name := "slick-effect",
  libraryDependencies ++= Seq(
    "com.typesafe.slick" %% "slick" % "3.3.2",
    "org.typelevel" %% "cats-effect" % "2.0.0-M1",
    "org.typelevel" %% "cats-testkit" % "2.0.0-M1" % Test,
    "org.typelevel" %% "cats-effect-laws" % "2.0.0-M1" % Test,
    "org.scalatest" %% "scalatest" % "3.0.8" % Test,
    "com.h2database" % "h2" % "1.4.199" % Test
  ) ++ compilerPlugins
)

val core = project.in(file(".")).settings(commonSettings)
