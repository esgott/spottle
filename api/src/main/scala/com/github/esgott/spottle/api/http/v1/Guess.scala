package com.github.esgott.spottle.api.http.v1

import com.github.esgott.spottle.api.Symbol
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class Guess(
    gameVersion: Int,
    symbol: Symbol
)

object Guess:
  given Codec[Guess] = deriveCodec
