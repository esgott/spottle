package com.github.esgott.spottle.api.http.v1


import com.github.esgott.spottle.api.Symbol
import io.circe.Codec


case class Guess(
    gameVersion: Int,
    symbol: Symbol
) derives Codec.AsObject
