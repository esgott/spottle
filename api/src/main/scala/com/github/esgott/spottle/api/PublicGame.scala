package com.github.esgott.spottle.api


import cats.syntax.option._
import io.circe.Codec
import sttp.tapir.Schema


case class PublicGame(
    version: Int,
    card: Card,
    playerCards: Map[Player, Option[Card]]
) derives Codec.AsObject


object PublicGame:
  import sttp.tapir.generic.auto._


  // Because of https://github.com/softwaremill/tapir/issues/918
  given Schema[Map[Player, Option[Card]]] =
    summon[Schema[Map[String, Option[Card]]]]
      .map(_.map { case (player, card) =>
        (Player(player), card)
      }.some)(_.map { case (player, card) =>
        (player.asInstanceOf[String], card)
      })

  given Schema[PublicGame] = Schema.derivedSchema[PublicGame]
