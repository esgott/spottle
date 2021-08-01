package com.github.esgott.spottle.api.http.v1


import com.github.esgott.spottle.api.Player
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec


case class CreateGame(order: Int, otherPlayers: Player)


object CreateGame:
  given Codec[CreateGame] = deriveCodec
