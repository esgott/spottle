package com.github.esgott.spottle.api.http.v1


import com.github.esgott.spottle.api.PublicGame
import io.circe.Codec


case class GameUpdate(gameId: Long, game: PublicGame) derives Codec.AsObject
