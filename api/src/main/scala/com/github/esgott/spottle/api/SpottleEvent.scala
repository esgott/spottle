package com.github.esgott.spottle.api


import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.auto._
import io.circe.syntax._


// TODO migrate to circe-generic-extras when Scala 3 is supported by it

enum SpottleEvent:
  case GameUpdate(gameId: Long, game: PublicGame, command: SpottleCommand)
  case Winner(gameId: Long, winner: Player, game: PublicGame)
  case ClientError(message: String, command: SpottleCommand)
  case InternalError(message: String, command: SpottleCommand)


object SpottleEvent:

  given Encoder[SpottleEvent] = { case event: GameUpdate =>
    event.asJson.deepMerge(typeDescriptor("GameUpdate"))
  }


  private def typeDescriptor(name: String) =
    Json.obj("type" -> Json.fromString(name))
