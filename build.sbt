ThisBuild / scalaVersion     := "2.13.10"
ThisBuild / version          := "0.2.0"
ThisBuild / organization     := "ANL"

val chiselVersion = "3.5.6"

lazy val root = (project in file("."))
  .settings(
    name := "StreamPressor",
    libraryDependencies ++= Seq(
      "org.apache.commons" % "commons-math3" % "3.6.1",
      "edu.berkeley.cs" %% "chisel3" % chiselVersion,
      "edu.berkeley.cs" %% "chiseltest" % "0.5.4" % "test"
      //"org.chipsalliance" %% "chisel" % chiselVersion,
      //"edu.berkeley.cs" %% "chiseltest" % chiselVersion % "test"
    ),
    scalacOptions ++= Seq(
      "-language:reflectiveCalls",
      "-deprecation",
      "-feature",
      "-Xcheckinit",
      "-P:chiselplugin:genBundleElements",
    ),
    addCompilerPlugin("edu.berkeley.cs" % "chisel3-plugin" % chiselVersion cross CrossVersion.full),
    //addCompilerPlugin("org.chipsalliance" % "chisel-plugin" % chiselVersion cross CrossVersion.full),
 )
