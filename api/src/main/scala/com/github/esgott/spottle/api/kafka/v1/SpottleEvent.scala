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
  case NotFound(gameId: Long, message: String, command: SpottleCommand)
  case GameHasAdvanced(gameId: Long, version: Int, newestVersion: Int, command: SpottleCommand)
  case GameAlreadyFinished(gameId: Long, command: SpottleCommand)
  case NotPlayersTurn(gameId: Long, player: Player, command: SpottleCommand)
  case SymbolsNotMatching(gameId: Long, symbol: Symbol, command: SpottleCommand)
  case InternalError(message: String, command: SpottleCommand)


object SpottleEvent:

  given Encoder[SpottleEvent] = {
    case event: GameUpdate          => event.asJson.deepMerge(typeDescriptor("GameUpdate"))
    case event: Winner              => event.asJson.deepMerge(typeDescriptor("Winner"))
    case event: NotFound            => event.asJson.deepMerge(typeDescriptor("NotFound"))
    case event: GameAlreadyFinished => event.asJson.deepMerge(typeDescriptor("GameAlreadyFinished"))
    case event: NotPlayersTurn      => event.asJson.deepMerge(typeDescriptor("NotPlayersTurn"))
    case event: SymbolsNotMatching  => event.asJson.deepMerge(typeDescriptor("SymbolsNotMatching"))
    case event: GameHasAdvanced     => event.asJson.deepMerge(typeDescriptor("GameHasAdvanced"))
    case event: InternalError       => event.asJson.deepMerge(typeDescriptor("InternalError"))
  }


  given Decoder[SpottleEvent] = { cursor =>
    for
      name <- cursor.downField("type").as[String]
      result <- name match
        case "GameUpdate"          => cursor.as[GameUpdate]
        case "Winner"              => cursor.as[Winner]
        case "NotFound"            => cursor.as[NotFound]
        case "GameHasAdvanced"     => cursor.as[GameHasAdvanced]
        case "GameAlreadyFinished" => cursor.as[GameAlreadyFinished]
        case "NotPlayersTurn"      => cursor.as[NotPlayersTurn]
        case "SymbolsNotMatching"  => cursor.as[SymbolsNotMatching]
        case "InternalError"       => cursor.as[InternalError]
        case other =>
          val history = DownField("type") :: cursor.history
          DecodingFailure(s"Unknown command type '$other'", history).asLeft
    yield result
  }
