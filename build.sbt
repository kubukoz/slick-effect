val Scala_2_11 = "2.11.12"
val Scala_2_12 = "2.12.8"
val Scala_2_13 = "2.13.0"

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
  List(compilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3"))

def below213(scalaVersion: String) = !scalaVersion.startsWith("2.13")

def catsEffectVersion(scalaVersion: String) = if (below213(scalaVersion)) "1.3.1" else "2.0.0-M4"

val commonSettings = Seq(
  scalaVersion := Scala_2_11,
  Options.addAll,
  fork in Test := true,
  crossScalaVersions := Seq(Scala_2_11, Scala_2_12, Scala_2_13),
  //todo uncomment after 2.13 release
  mimaPreviousArtifacts := Set.empty /*(Set(organization.value %% name.value % "0.1.0"))*/,
  libraryDependencies ++= Seq(
    "com.typesafe.slick" %% "slick"            % "3.3.2",
    "org.typelevel"      %% "cats-effect"      % catsEffectVersion(scalaVersion.value),
    "org.typelevel"      %% "cats-testkit"     % "2.0.0-RC1" % Test,
    "org.typelevel"      %% "cats-effect-laws" % catsEffectVersion(scalaVersion.value) % Test,
    "org.scalatest"      %% "scalatest"        % "3.0.8" % Test,
    "com.h2database"     % "h2"                % "1.4.199" % Test
  ) ++ compilerPlugins(scalaVersion.value)
)

val core       = project.settings(commonSettings, name := "slick-effect")
val transactor = project.settings(commonSettings, name := "slick-effect-transactor")

val root =
  project.in(file(".")).settings(publishArtifact := false, scalaVersion := Scala_2_11).aggregate(core, transactor)
