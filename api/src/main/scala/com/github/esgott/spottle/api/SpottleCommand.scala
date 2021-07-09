package com.github.esgott.spottle.api


import cats.data.NonEmptyList
import cats.syntax.either._
import io.circe.{Decoder, DecodingFailure}
import io.circe.CursorOp.DownField
import io.circe.generic.auto._


// TODO migrate to circe-generic-extras when Scala 3 is supported by it

enum SpottleCommand:
  case CreateGame(gameId: Long, order: Int, creator: Player, players: NonEmptyList[Player])
  case GetGame(gameId: Long, player: Player)
  case Guess(gameId: Long, gameVersion: Int, player: Player, symbol: Symbol)
  case FinishGame(gameId: Long, player: Player)


object SpottleCommand:

  given Decoder[SpottleCommand] = { cursor =>
    for
      name <- cursor.downField("type").as[String]
      result <- name match
        case "CreateGame" => cursor.as[CreateGame]
        case "GetGame"    => cursor.as[GetGame]
        case "Guess"      => cursor.as[Guess]
        case "FinishGame" => cursor.as[FinishGame]
        case other =>
          DecodingFailure(s"Unknown command type '$other'", List(DownField("type"))).asLeft
    yield result
  }
