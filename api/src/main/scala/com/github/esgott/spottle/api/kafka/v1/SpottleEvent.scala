package com.github.esgott.spottle.api.kafka.v1


import cats.syntax.either._
import com.github.esgott.spottle.api._
import com.github.esgott.spottle.api.circe.typeDescriptor
import io.circe.CursorOp.DownField
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{Decoder, DecodingFailure, Encoder, Json}


// TODO migrate to codec derivation when https://github.com/circe/circe/issues/1777 is solved

enum SpottleEvent:
  case GameUpdate(gameId: Long, game: PublicGame, command: SpottleCommand)
  case Winner(gameId: Long, winner: Player, game: PublicGame)
  case ClientError(message: String, command: SpottleCommand)
  case InternalError(message: String, command: SpottleCommand)


object SpottleEvent:

  given Encoder[SpottleEvent] = {
    case event: GameUpdate    => event.asJson.deepMerge(typeDescriptor("GameUpdate"))
    case event: Winner        => event.asJson.deepMerge(typeDescriptor("Winner"))
    case event: ClientError   => event.asJson.deepMerge(typeDescriptor("ClientError"))
    case event: InternalError => event.asJson.deepMerge(typeDescriptor("InternalError"))
  }


  given Decoder[SpottleEvent] = { cursor =>
    for
      name <- cursor.downField("type").as[String]
      result <- name match
        case "GameUpdate"    => cursor.as[GameUpdate]
        case "Winner"        => cursor.as[Winner]
        case "ClientError"   => cursor.as[ClientError]
        case "InternalError" => cursor.as[InternalError]
        case other =>
          DecodingFailure(s"Unknown command type '$other'", List(DownField("type"))).asLeft
    yield result
  }
