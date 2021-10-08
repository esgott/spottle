package com.github.esgott.spottle.engine


import cats.effect.{Async, Ref}
import cats.syntax.all._
import com.github.esgott.spottle.service.Endpoints
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s.Http4sServerInterpreter


trait EngineEndpoints[F[_]]:
  def routes: HttpRoutes[F]


object EngineEndpoints:

  def engineEndpoints[F[_]: Async](ready: Ref[F, Boolean]): EngineEndpoints[F] =
    new EngineEndpoints[F]:

      override def routes =
        Endpoints.healthRoute[F] <+>
          Endpoints.readyRoute[F](ready)
