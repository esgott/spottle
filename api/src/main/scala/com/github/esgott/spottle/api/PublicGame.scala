package com.github.esgott.spottle.api


import cats.data.NonEmptyMap
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import sttp.tapir.Schema


case class PublicGame(
    version: Int,
    card: Card,
    playerCards: NonEmptyMap[Player, Option[Card]]
)


object PublicGame:
  given Codec[PublicGame] = deriveCodec

  given Schema[PublicGame] = summon[Schema[PublicGame]]
