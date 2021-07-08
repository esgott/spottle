package com.github.esgott.spottle.core


case class Game(
    version: Int,
    cards: List[Card],
    players: List[Player],
    playerCards: Map[Player, List[Card]],
    nextPlayer: Player
)


type Card = Set[Symbol]

opaque type Symbol = String


object Symbol {
  def apply(s: String): Symbol = s
}


opaque type Player = String


object Player {
  def apply(s: String): Player = s
}
