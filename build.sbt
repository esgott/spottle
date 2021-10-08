ThisBuild / scalaVersion := "3.0.2"
ThisBuild / organization := "com.github.esgott"

ThisBuild / testFrameworks += new TestFramework("weaver.framework.CatsEffect")

ThisBuild / evictionErrorLevel := Level.Warn


lazy val spottle = (project in file("."))
  .aggregate(
    `spottle-api`,
    `spottle-core`,
    `spottle-edge`,
    `spottle-engine`,
    `spottle-kafka`,
    `spottle-service`
  )


lazy val `spottle-api` = (project in file("api"))
  .settings(
    libraryDependencies ++= Dependencies.api
  )


lazy val `spottle-core` = (project in file("core"))
  .dependsOn(`spottle-api`)
  .settings(
    libraryDependencies ++= Dependencies.core
  )


lazy val `spottle-edge` = (project in file("edge"))
  .enablePlugins(DockerPlugin, JavaAppPackaging)
  .dependsOn(
    `spottle-core`,
    `spottle-kafka`,
    `spottle-service`
  )
  .settings(
    libraryDependencies ++= Dependencies.edge,
    dockerExposedPorts := List(8080)
  )


lazy val `spottle-engine` = (project in file("engine"))
  .enablePlugins(DockerPlugin, JavaAppPackaging)
  .dependsOn(
    `spottle-core`,
    `spottle-kafka`,
    `spottle-service`
  )
  .settings(
    libraryDependencies ++= Dependencies.engine,
    dockerExposedPorts := List(8080)
  )


lazy val `spottle-kafka` = (project in file("kafka"))
  .settings(
    libraryDependencies ++= Dependencies.kafka
  )


lazy val `spottle-service` = (project in file("service"))
  .settings(
    libraryDependencies ++= Dependencies.service
  )
