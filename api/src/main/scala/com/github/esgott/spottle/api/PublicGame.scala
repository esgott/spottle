package com.github.esgott.spottle.api


import cats.data.NonEmptyMap
import io.circe.Codec
import sttp.tapir.Schema


case class PublicGame(
    version: Int,
    card: Card,
    playerCards: NonEmptyMap[Player, Option[Card]]
) derives Codec.AsObject


object PublicGame:

  given Schema[PublicGame] = summon[Schema[PublicGame]]
