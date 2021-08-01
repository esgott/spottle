package com.github.esgott.spottle.api.kafka.v1


import cats.data.NonEmptyList
import cats.syntax.either._
import com.github.esgott.spottle.api._
import com.github.esgott.spottle.api.circe.typeDescriptor
import io.circe.CursorOp.DownField
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{Decoder, DecodingFailure, Encoder}


// TODO migrate to circe-generic-extras when https://github.com/circe/circe-generic-extras/issues/168 is solved

enum SpottleCommand:
  case CreateGame(gameId: Long, order: Int, creator: Player, players: NonEmptyList[Player])
  case GetGame(gameId: Long, player: Player)
  case Guess(gameId: Long, gameVersion: Int, player: Player, symbol: Symbol)
  case FinishGame(gameId: Long, player: Player)


object SpottleCommand:

  given Encoder[SpottleCommand] = {
    case command: CreateGame => command.asJson.deepMerge(typeDescriptor("CreateGame"))
    case command: GetGame    => command.asJson.deepMerge(typeDescriptor("GetGame"))
    case command: Guess      => command.asJson.deepMerge(typeDescriptor("Guess"))
    case command: FinishGame => command.asJson.deepMerge(typeDescriptor("FinishGame"))
  }


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
