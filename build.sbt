ThisBuild / tlBaseVersion := "0.6"
ThisBuild / organization := "com.kubukoz"
ThisBuild / organizationName := "Jakub Kozłowski"
ThisBuild / startYear := Some(2019)
ThisBuild / licenses := Seq(License.Apache2)
ThisBuild / developers := List(tlGitHubDev("kubukoz", "Jakub Kozłowski"))
ThisBuild / sonatypeCredentialHost := Sonatype.sonatypeLegacy

val Scala_2_12 = "2.12.20"
val Scala_2_13 = "2.13.16"

val Scala_3 = "3.3.5"

val catsEffectVersion = "3.6.1"

ThisBuild / scalaVersion := Scala_2_12
ThisBuild / crossScalaVersions := Seq(Scala_2_12, Scala_2_13, Scala_3)

ThisBuild / tlFatalWarnings := false

def compilerPlugins(scalaVersion: String) =
  if (scalaVersion.startsWith("3.")) Nil
  else
    compilerPlugin(
      "org.typelevel" % "kind-projector" % "0.13.3" cross CrossVersion.full
    ) :: Nil

val commonSettings = Seq(
  Test / fork := true,
  libraryDependencies ++= Seq(
    "com.typesafe.slick" %% "slick" % "3.6.0",
    "org.typelevel" %% "cats-effect-kernel" % catsEffectVersion,
    "org.typelevel" %% "cats-effect-std" % catsEffectVersion,
    "org.typelevel" %% "cats-testkit" % "2.13.0" % Test,
    "org.typelevel" %% "cats-effect-laws" % catsEffectVersion % Test,
    "org.typelevel" %% "cats-effect-testkit" % catsEffectVersion % Test,
    "com.h2database" % "h2" % "2.2.224" % Test,
    "org.typelevel" %% "cats-testkit-scalatest" % "2.1.5" % Test
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

def versionSpecificOptions(scalaVersion: String) =
  if (scalaVersion.startsWith("2.13"))
    List("-Ymacro-annotations")
  else
    Nil

val examples = project
  .settings(
    commonSettings,
    publish / skip := true,
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect-kernel" % catsEffectVersion,
      "org.typelevel" %% "cats-tagless-core" % "0.16.3",
      "org.postgresql" % "postgresql" % "42.7.3"
    ),
    mimaPreviousArtifacts := Set.empty
  )
  .settings(scalacOptions ++= versionSpecificOptions(scalaVersion.value))
  .dependsOn(core, catsio, transactor)

val root =
  project
    .in(file("."))
    .settings(
      publish / skip := true
    )
    .enablePlugins(NoPublishPlugin)
    .aggregate(core, catsio, transactor, examples)
