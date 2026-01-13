ThisBuild / scalaVersion     := "2.13.18"
ThisBuild / version          := "0.3.0"
ThisBuild / organization     := "ANL"

val chiselVersion = "7.6.0"
val scalatestVersion = "3.2.18"

lazy val root = (project in file("."))
  .settings(
    name := "StreamPressor",
    resolvers ++= Seq(
      Resolver.mavenLocal,
      Resolver.sonatypeCentralSnapshots
    ),
    libraryDependencies ++= Seq(
      "org.apache.commons" % "commons-math3" % "3.6.1",
      "org.chipsalliance" %% "chisel" % chiselVersion,
      "org.scalatest" %% "scalatest" % scalatestVersion % "test",
      "me.shadaj" %% "scalapy-core" % "0.5.2"
    ),
    scalacOptions ++= Seq(
      "-language:reflectiveCalls",
      "-deprecation",
      "-feature",
      "-Xcheckinit",
    ),
    addCompilerPlugin("org.chipsalliance" % "chisel-plugin" % chiselVersion cross CrossVersion.full),
)
