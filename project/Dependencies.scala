import sbt._


object Dependencies {

  private def scalaDep(org: String, prefix: String, version: String): String => ModuleID =
    suffix => org %% depName(prefix, suffix) % version


  private def depName(prefix: String, suffix: String) =
    if (suffix.isEmpty) prefix
    else if (prefix.isEmpty) suffix
    else s"$prefix-$suffix"


  private val cats       = scalaDep("org.typelevel", "cats", "2.6.1")
  private val catsEffect = scalaDep("org.typelevel", "cats-effect", "3.2.8")
  private val circe      = scalaDep("io.circe", "circe", "0.14.0")
  private val fs2        = scalaDep("co.fs2", "fs2", "3.1.1")
  private val http4s     = scalaDep("org.http4s", "http4s", "0.23.3")
  private val log4cats   = scalaDep("org.typelevel", "log4cats", "2.1.1")
  private val tapir      = scalaDep("com.softwaremill.sttp.tapir", "tapir", "0.19.0-M8")
  private val weaver     = scalaDep("com.disneystreaming", "weaver", "0.7.4")

  private val fs2Kafka = "com.github.fd4s" %% "fs2-kafka"       % "2.1.0"
  private val ciris    = "is.cir"          %% "ciris"           % "2.1.1"
  private val logback  = "ch.qos.logback"   % "logback-classic" % "1.2.6"


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
    fs2("core"),
    weaver("cats") % Test
  )


  val engine = Seq(
    fs2("core"),
    weaver("cats") % Test
  )


  val kafka = Seq(
    circe("fs2"),
    circe("generic"),
    fs2Kafka
  )


  val service = Seq(
    catsEffect(""),
    ciris, // circe-config is not yet publish for Scala 3 and Cats Effect 3
    http4s("core"),
    http4s("dsl"),
    log4cats("slf4j"),
    logback,
    tapir("http4s-server")
  )

}
