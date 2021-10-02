package com.github.esgott.spottle.api.http.v1

import com.github.esgott.spottle.api.Symbol


case class Guess(
    gameId: Long,
    gameVersion: Int,
    symbol: Symbol
)
