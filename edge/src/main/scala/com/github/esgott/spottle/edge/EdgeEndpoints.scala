package com.github.esgott.spottle.edge


import cats.data.EitherT
import cats.effect.{Async, Ref}
import cats.syntax.all._
import com.github.esgott.spottle.api.http.v1.{Http, SpottleError}
import com.github.esgott.spottle.api.http.v1.Http.AuthenticatedSpottleEndpoint
import com.github.esgott.spottle.api.Player
import org.http4s.HttpRoutes
import sttp.tapir.Endpoint
import sttp.tapir.server.http4s.Http4sServerInterpreter


trait EdgeEndpoints[F[_]]:
  def routes: HttpRoutes[F]


object EdgeEndpoints:

  def edgeEndpoints[F[_]: Async: EdgeHttp](ready: Ref[F, Boolean]): EdgeEndpoints[F] =
    new EdgeEndpoints[F]:

      private val http = summon[EdgeHttp[F]]


      override def routes: HttpRoutes[F] =
        authenticatedEndpoint(Http.createGame, http.createGame) <+>
          authenticatedEndpoint(Http.getGame, http.getGame) <+>
          authenticatedEndpoint(Http.pollGame, http.pollGame) <+>
          authenticatedEndpoint(Http.guess, http.guess)


      private def authenticatedEndpoint[In, Out](
          endpoint: AuthenticatedSpottleEndpoint[In, Out],
          handler: (In, Player) => EdgeHttp.Result[F, Out]
      ) =
        Http4sServerInterpreter[F]().toRoutes(endpoint) { case (request, player) =>
          handler(request, player).value
        }
