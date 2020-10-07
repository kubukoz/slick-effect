val Scala_2_12 = "2.12.12"
val Scala_2_13 = "2.13.3"

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
  List(compilerPlugin("org.typelevel" % "kind-projector" % "0.11.0" cross CrossVersion.full))

val commonSettings = Seq(
  scalaVersion := Scala_2_12,
  Options.addAll,
  fork in Test := true,
  crossScalaVersions := Seq(Scala_2_12, Scala_2_13),
  //uncomment after release for CE3
  mimaPreviousArtifacts := (Set( /* organization.value %% name.value % "0.1.0" */ )),
  libraryDependencies ++= Seq(
    "com.typesafe.slick" %% "slick"                  % "3.3.3",
    "org.typelevel"      %% "cats-effect"            % "3.0.0-M1",
    "org.typelevel"      %% "cats-testkit"           % "2.0.0" % Test,
    "org.typelevel"      %% "cats-effect-laws"       % "3.0.0-M1" % Test,
    "org.typelevel"      %% "cats-effect-testkit"    % "3.0.0-M1" % Test,
    "com.h2database"     % "h2"                      % "1.4.200" % Test,
    "org.typelevel"      %% "cats-testkit-scalatest" % "1.0.0" % Test
  ) ++ compilerPlugins(scalaVersion.value)
)

val core       = project.settings(commonSettings, name := "slick-effect")
val transactor = project.settings(commonSettings, name := "slick-effect-transactor")

val root =
  project
    .in(file("."))
    .settings(mimaPreviousArtifacts := Set.empty, publishArtifact := false, scalaVersion := Scala_2_12)
    .aggregate(core, transactor)
