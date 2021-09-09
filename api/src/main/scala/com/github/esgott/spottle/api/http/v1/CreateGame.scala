package com.github.esgott.spottle.api.http.v1


import com.github.esgott.spottle.api.Player
import io.circe.Codec


case class CreateGame(order: Int, otherPlayers: Player) derives Codec.AsObject
