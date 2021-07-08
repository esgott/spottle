import sbt._


object Dependencies {

  private val catsEffect = "org.typelevel"       %% "cats-effect" % "3.1.1"
  private val weaverCats = "com.disneystreaming" %% "weaver-cats" % "0.7.3"


  private val breeze = ("org.scalanlp" %% "breeze" % "1.2")
    .cross(CrossVersion.for3Use2_13)
    .exclude("org.typelevel", "cats-kernel_2.13")


  val core = Seq(
    breeze,
    catsEffect,
    weaverCats % Test
  )

}
