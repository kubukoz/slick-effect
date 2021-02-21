val Scala_2_12 = "2.12.13"
val Scala_2_13 = "2.13.4"

val catsEffectVersion = "3.0.0-RC2"

inThisBuild(
  List(
    organization := "com.kubukoz",
    homepage := Some(url("https://github.com/kubukoz/slick-effect")),
    licenses := List(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
    developers := List(
      Developer(
        "kubukoz",
        "Jakub Kozłowski",
        "kubukoz@gmail.com",
        url("https://kubukoz.com")
      )
    )
  )
)

def compilerPlugins(scalaVersion: String) =
  List(
    compilerPlugin(
      "org.typelevel" % "kind-projector" % "0.11.3" cross CrossVersion.full
    )
  )

val commonSettings = Seq(
  scalaVersion := Scala_2_12,
  Options.addAll,
  fork in Test := true,
  crossScalaVersions := Seq(Scala_2_12, Scala_2_13),
  //uncomment after release for CE3
  mimaPreviousArtifacts := (Set(
    /* organization.value %% name.value % "0.1.0" */
  )),
  libraryDependencies ++= Seq(
    "com.typesafe.slick" %% "slick" % "3.3.3",
    "org.typelevel" %% "cats-effect-kernel" % catsEffectVersion,
    "org.typelevel" %% "cats-effect-std" % catsEffectVersion,
    "org.typelevel" %% "cats-testkit" % "2.3.1" % Test,
    "org.typelevel" %% "cats-effect-laws" % catsEffectVersion % Test,
    "org.typelevel" %% "cats-effect-testkit" % catsEffectVersion % Test,
    "com.h2database" % "h2" % "1.4.200" % Test,
    "org.typelevel" %% "cats-testkit-scalatest" % "2.1.1" % Test
  ) ++ compilerPlugins(scalaVersion.value)
)

val core = project.settings(commonSettings, name := "slick-effect")

val catsio = project.settings(
  commonSettings,
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-effect" % catsEffectVersion
  ),
  name := "slick-effect-catsio"
)

val transactor =
  project.settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % catsEffectVersion % Test
    ),
    name := "slick-effect-transactor"
  )

val examples = project
  .settings(
    commonSettings,
    publishArtifact := false,
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % catsEffectVersion,
      "org.typelevel" %% "cats-tagless-macros" % "0.12",
      "org.postgresql" % "postgresql" % "42.2.18",
      compilerPlugin(
        "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full
      )
    ),
    mimaPreviousArtifacts := Set.empty
  )
  .dependsOn(core, catsio, transactor)

val root =
  project
    .in(file("."))
    .settings(
      mimaPreviousArtifacts := Set.empty,
      publishArtifact := false,
      scalaVersion := Scala_2_12
    )
    .aggregate(core, catsio, transactor, examples)
