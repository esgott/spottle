import sbt._


object Dependencies {

  private val catsCore   = "org.typelevel"       %% "cats-core"   % "2.6.1"
  private val catsEffect = "org.typelevel"       %% "cats-effect" % "3.1.1"
  private val weaverCats = "com.disneystreaming" %% "weaver-cats" % "0.7.4"


  private val breeze = ("org.scalanlp" %% "breeze" % "1.2")
    .cross(CrossVersion.for3Use2_13)
    .exclude("org.typelevel", "cats-kernel_2.13")


  val api = Seq(
    catsCore
  )


  val core = Seq(
    breeze,
    catsCore,
    weaverCats % Test
  )

}
