import sbt._


object Dependencies {

  private def scalaDep(org: String, prefix: String, version: String): String => ModuleID =
    suffix => org %% depName(prefix, suffix) % version


  private def depName(prefix: String, suffix: String) =
    if (suffix.isEmpty) prefix
    else if (prefix.isEmpty) suffix
    else s"$prefix-$suffix"


  private val cats       = scalaDep("org.typelevel", "cats", "2.6.1")
  private val catsEffect = scalaDep("org.typelevel", "cats-effect", "3.2.3")
  private val circe      = scalaDep("io.circe", "circe", "0.14.0")
  private val fs2        = scalaDep("co.fs2", "fs2", "3.1.0")
  private val http4s     = scalaDep("org.http4s", "http4s", "0.23.1")
  private val tapir      = scalaDep("com.softwaremill.sttp.tapir", "tapir", "0.18.3")
  private val weaver     = scalaDep("com.disneystreaming", "weaver", "0.7.4")

  private val fs2Kafka = "com.github.fd4s" %% "fs2-kafka" % "2.1.0"


  private val breeze = ("org.scalanlp" %% "breeze" % "2.0-RC3")
    .exclude("org.typelevel", "cats-kernel_2.13")


  val api = Seq(
    cats("core"),
    circe("generic"),
    tapir("core"),
    tapir("json-circe")
  )


  val core = Seq(
    breeze,
    cats("core"),
    weaver("cats") % Test
  )


  val edge = Seq(
    catsEffect(""),
    fs2("core"),
    http4s("core"),
    tapir("http4s-server"),
    weaver("cats") % Test
  )


  val engine = Seq(
    catsEffect(""),
    fs2("core"),
    weaver("cats") % Test
  )


  val kafka = Seq(
    circe("fs2"),
    circe("generic"),
    fs2Kafka
  )

}
