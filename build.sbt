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

def compilerPlugins(scalaVersion: String) = {
  val paradise =
    if (below213(scalaVersion))
      List(compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full))
    else Nil

  List(compilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3")) ++ paradise
}

def below213(scalaVersion: String) = !scalaVersion.startsWith("2.13")

val commonSettings = Seq(
  scalaVersion := Scala_2_11,
  Options.addAll,
  fork in Test := true,
  crossScalaVersions := Seq(Scala_2_11, Scala_2_12, Scala_2_13),
  name ~= ("slick-effect-" + _),
  //todo uncomment after 2.13 release
  mimaPreviousArtifacts := Set.empty/*(Set(organization.value %% name.value % "0.1.0"))*/,
  libraryDependencies ++= Seq(
    "com.typesafe.slick"   %% "slick"            % "3.3.2",
    "com.github.mpilquist" %% "simulacrum"       % "0.19.0",
    "org.typelevel"        %% "cats-effect"      % "2.0.0-M4",
    "org.typelevel"        %% "cats-testkit"     % "2.0.0-M4" % Test,
    "org.typelevel"        %% "cats-effect-laws" % "2.0.0-M4" % Test,
    "org.scalatest"        %% "scalatest"        % "3.0.8" % Test,
    "com.h2database"       % "h2"                % "1.4.199" % Test
  ) ++ compilerPlugins(scalaVersion.value)
)

val core       = project.settings(commonSettings)
val transactor = project.settings(commonSettings)

val root = project
  .in(file("."))
  .settings(
    publishArtifact := false
  )
  .aggregate(core, transactor)
