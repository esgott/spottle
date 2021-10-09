package com.github.esgott.spottle.service


import cats.effect.{IO, Ref}
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import org.http4s.server.middleware.{Logger => LoggerMiddleware}
import org.typelevel.log4cats.Logger

import scala.concurrent.ExecutionContext


object Endpoints:

  private def diagRoutes(ready: Ref[IO, Boolean]) = HttpRoutes.of[IO] {
    case GET -> Root / "health" =>
      Ok()
    case GET -> Root / "ready" =>
      for
        r      <- ready.get
        result <- if (r) Ok() else NotFound()
      yield result
  }


  def server(
      port: Int,
      routes: HttpRoutes[IO],
      ready: Ref[IO, Boolean],
      logger: Logger[IO]
  ): BlazeServerBuilder[IO] = {
    val loggedRoutes = LoggerMiddleware.httpRoutes[IO](
      logHeaders = true,
      logBody = true,
      logAction = Some(msg => logger.debug(msg))
    )(routes)

    BlazeServerBuilder[IO](ExecutionContext.global)
      .bindHttp(port, "0.0.0.0")
      .withHttpApp(Router("/" -> loggedRoutes, "diag" -> diagRoutes(ready)).orNotFound)
  }


  def diagServer(port: Int, ready: Ref[IO, Boolean]): BlazeServerBuilder[IO] =
    BlazeServerBuilder[IO](ExecutionContext.global)
      .bindHttp(port, "0.0.0.0")
      .withHttpApp(Router("diag" -> diagRoutes(ready)).orNotFound)
