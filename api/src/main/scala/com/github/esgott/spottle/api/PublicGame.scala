package com.github.esgott.spottle.api

import cats.data.NonEmptyMap


case class PublicGame(
    version: Int,
    card: Card,
    playerCards: NonEmptyMap[Player, Card]
)
