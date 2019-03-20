val Scala_2_11 = "2.11.11"
val Scala_2_12 = "2.12.8"

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
  compilerPlugin("org.scalamacros" % "paradise" % "2.1.1").cross(CrossVersion.full),
  compilerPlugin("org.spire-math" %% "kind-projector" % "0.9.9")
)

val commonSettings = Seq(
  scalaVersion := "2.11.11",
  Options.addAll,
  fork in Test := true,
  crossScalaVersions := Seq(Scala_2_11, Scala_2_12),
  name := "slick-effect",
  libraryDependencies ++= Seq(
    "com.typesafe.slick" %% "slick" % "3.3.0",
    "org.typelevel" %% "cats-effect" % "1.2.0",
    "org.typelevel" %% "cats-testkit" % "1.6.0" % Test,
    "org.typelevel" %% "cats-effect-laws" % "1.2.0" % Test,
    "org.scalatest" %% "scalatest" % "3.0.7" % Test,
    "com.h2database" % "h2" % "1.4.199" % Test
  ) ++ compilerPlugins
)

val core = project.in(file(".")).settings(commonSettings)
