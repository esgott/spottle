ThisBuild / scalaVersion := "3.0.0"
ThisBuild / organization := "com.github.esgott"

ThisBuild / testFrameworks += new TestFramework("weaver.framework.CatsEffect")


lazy val spottle = (project in file("."))
  .aggregate(
    `spottle-api`,
    `spottle-core`
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
