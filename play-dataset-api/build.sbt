name := """play-dataset-api"""
organization := "sanketika"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.17"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.2" % Test

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "sanketika.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "sanketika.binders._"

libraryDependencies ++= Seq(
  guice,
  "com.datastax.oss" % "java-driver-core" % "4.17.0"
)

