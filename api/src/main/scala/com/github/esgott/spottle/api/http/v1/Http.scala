package com.github.esgott.spottle.api.http.v1


import com.github.esgott.spottle.api._
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.tapir.generic.auto._

import scala.deriving.Mirror


object Http:

  type SpottleEndpoint[In, Out]              = Endpoint[In, SpottleError, Out, Any]
  type AuthenticatedSpottleEndpoint[In, Out] = SpottleEndpoint[(In, Player), Out]

  private val gameId      = path[Long]
  private val gameVersion = path[Int]
  private val symbol      = path[Symbol]


  extension [In, Out](e: SpottleEndpoint[In, Out])

    def authenticated: AuthenticatedSpottleEndpoint[In, Out] =
      e.in(header[Player]("player"))


  def mapTo[CC <: Product](using m: Mirror.ProductOf[CC]): Mapping[m.MirroredElemTypes, CC] =
    Mapping.from(m.fromProduct)(Tuple.fromProductTyped)


  private val baseEndpoint =
    endpoint
      .errorOut(oneOf[SpottleError](
        oneOfMapping(StatusCode.NotFound, jsonBody[SpottleError.NotFound]),
        oneOfMapping(StatusCode.BadRequest, jsonBody[SpottleError.BadRequest])
      ))


  val createGame: AuthenticatedSpottleEndpoint[CreateGame, GameUpdate] =
    baseEndpoint.post
      .in("game")
      .in(jsonBody[CreateGame])
      .out(jsonBody[GameUpdate])
      .authenticated


  val getGame: AuthenticatedSpottleEndpoint[Long, GameUpdate] =
    baseEndpoint.get
      .in("game" / gameId)
      .out(jsonBody[GameUpdate])
      .authenticated


  val pollGame: AuthenticatedSpottleEndpoint[Long, GameUpdate] =
    baseEndpoint.get
      .in("game" / gameId / "poll")
      .out(jsonBody[GameUpdate])
      .authenticated


  val guess: AuthenticatedSpottleEndpoint[Guess, GameUpdate] =
    baseEndpoint.post
      .in("game" / gameId / "guess" / gameVersion / symbol)
      .mapIn(mapTo[Guess])
      .out(jsonBody[GameUpdate])
      .authenticated
