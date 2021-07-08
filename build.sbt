ThisBuild / scalaVersion := "3.0.0"
ThisBuild / organization := "com.github.esgott"

ThisBuild / testFrameworks += new TestFramework("weaver.framework.CatsEffect")


lazy val spottle = (project in file("."))
  .aggregate(
    `spottle-core`
  )


lazy val `spottle-core` = (project in file("core"))
  .settings(
    libraryDependencies ++= Dependencies.core
  )
