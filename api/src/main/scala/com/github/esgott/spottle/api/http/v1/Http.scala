package com.github.esgott.spottle.api.http.v1


import com.github.esgott.spottle.api._
import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.tapir.generic.auto._


object Http:
  type SpottleEndpoint[In, Out] = Endpoint[In, SpottleError, Out, Nothing]


  val baseEndpoint =
    endpoint
      .errorOut(jsonBody[SpottleError])
      .in(header[Player]("player"))


  val createGame: SpottleEndpoint[(Player, CreateGame), GameUpdate] =
    baseEndpoint.post
      .in("game")
      .in(jsonBody[CreateGame])
      .out(jsonBody[GameUpdate])


  val getGame: SpottleEndpoint[(Player, Long), GameUpdate] =
    baseEndpoint.get
      .in("game" / path[Long])
      .out(jsonBody[GameUpdate])


  val pollGame: SpottleEndpoint[(Player, Long), GameUpdate] =
    baseEndpoint.get
      .in("game" / path[Long] / "poll")
      .out(jsonBody[GameUpdate])


  val guess: SpottleEndpoint[(Player, Long, Guess), GameUpdate] =
    baseEndpoint.post
      .in("game" / path[Long] / "guess")
      .in(jsonBody[Guess])
      .out(jsonBody[GameUpdate])
