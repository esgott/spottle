package com.github.esgott.spottle.api.http.v1


import com.github.esgott.spottle.api.PublicGame
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec


case class GameUpdate(gameId: Long, game: PublicGame)


object GameUpdate:
  given Codec[GameUpdate] = deriveCodec
