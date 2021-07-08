package com.github.esgott.spottle.api

import cats.data.NonEmptyList


enum SpottleCommand:
  case CreateGame(gameId: Long, order: Int, players: NonEmptyList[Player])
  case GetGame(gameId: Long, player: Player)
  case Guess(gameId: Long, gameVersion: Int, player: Player, symbol: Symbol)
  case FinishGame(gameId: Long)
