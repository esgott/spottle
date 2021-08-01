package com.github.esgott.spottle.api.kafka.v1


import com.github.esgott.spottle.api._
import com.github.esgott.spottle.api.circe.typeDescriptor
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}


// TODO migrate to circe-generic-extras when https://github.com/circe/circe-generic-extras/issues/168 is solved

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
