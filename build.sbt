ThisBuild / tlBaseVersion := "0.6"
ThisBuild / organization := "com.kubukoz"
ThisBuild / organizationName := "Jakub Kozłowski"
ThisBuild / startYear := Some(2019)
ThisBuild / licenses := Seq(License.Apache2)
ThisBuild / developers := List(tlGitHubDev("kubukoz", "Jakub Kozłowski"))
ThisBuild / tlSonatypeUseLegacyHost := true

val Scala_2_12 = "2.12.17"
val Scala_2_13 = "2.13.13"

val Scala_3 = "3.3.3"

val catsEffectVersion = "3.5.4"

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
  // uncomment after release for CE3
  mimaPreviousArtifacts := Set(
    /* organization.value %% name.value % "0.1.0" */
  ),
  libraryDependencies ++= Seq(
    "com.typesafe.slick" %% "slick" % "3.5.1",
    "org.typelevel" %% "cats-effect-kernel" % catsEffectVersion,
    "org.typelevel" %% "cats-effect-std" % catsEffectVersion,
    "org.typelevel" %% "cats-testkit" % "2.10.0" % Test,
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
      "org.typelevel" %% "cats-tagless-core" % "0.15.0",
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
