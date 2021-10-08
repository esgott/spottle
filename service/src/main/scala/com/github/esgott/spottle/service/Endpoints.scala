package com.github.esgott.spottle.service


import cats.effect.{Async, Ref}
import cats.syntax.all._
import org.http4s.HttpRoutes
import org.http4s.blaze.server._
import org.http4s.server.Router
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.server.http4s.Http4sServerInterpreter

import scala.concurrent.ExecutionContext


object Endpoints:

  val health: Endpoint[Unit, Unit, Unit, Any] =
    endpoint.get.in("health")


  val ready: Endpoint[Unit, Unit, Unit, Any] =
    endpoint.get.in("ready").errorOut(statusCode(StatusCode.NotFound))


  def healthRoute[F[_]: Async]: HttpRoutes[F] =
    Http4sServerInterpreter[F]().toRoutes(health)(_ => ().asRight.pure)


  def readyRoute[F[_]: Async](readyRef: Ref[F, Boolean]): HttpRoutes[F] =
    Http4sServerInterpreter[F]().toRoutes(ready) { _ =>
      for r <- readyRef.get
      yield Either.cond(r, (), ())
    }


  def server[F[_]: Async](port: Int, routes: HttpRoutes[F]): BlazeServerBuilder[F] =
    BlazeServerBuilder[F](ExecutionContext.global)
      .bindHttp(port)
      .withHttpApp(Router("/" -> routes).orNotFound)
