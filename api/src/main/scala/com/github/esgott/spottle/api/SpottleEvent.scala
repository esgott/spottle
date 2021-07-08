package com.github.esgott.spottle.api


enum SpottleEvent:
  case GameUpdate(gameId: Long, forPlayer: Player, game: PublicGame, command: SpottleCommand)
  case Winner(gameId: Long, forPlayer: Player, winner: Player, game: PublicGame)
