package com.github.esgott.spottle.api.http.v1


import cats.data.NonEmptyList
import com.github.esgott.spottle.api.Player
import io.circe.Codec


case class CreateGame(order: Int, otherPlayers: NonEmptyList[Player]) derives Codec.AsObject
