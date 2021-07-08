package com.github.esgott.spottle.core


import cats.Order
import cats.data.{NonEmptyList, NonEmptyMap}
import cats.syntax.all._
import cats.instances.either._
import com.github.esgott.spottle.api.{Card, Player, Symbol}


case class Game(
    version: Int,
    cards: NonEmptyList[Card],
    playerCards: NonEmptyMap[Player, List[Card]],
    nextPlayer: Player
):

  val players: NonEmptyList[Player] = playerCards.keys.toNonEmptyList


  def playerAfter(previousPlayer: Player): GameResult[Player] =
    (players :+ players.head).toList
      .dropWhile(_ != previousPlayer)
      .drop(1)
      .take(1)
      .headOption
      .liftTo[GameResult](GameError.UnknownPlayer)


  def getCardsFor(player: Player): GameResult[NonEmptyList[Card]] =
    for
      cardList <- playerCards.lookup(player).liftTo[GameResult](GameError.UnknownPlayer)
      cardsNel <- NonEmptyList.fromList(cardList).liftTo[GameResult](GameError.GameAlreadyFinished)
    yield cardsNel


  def guess(player: Player, symbol: Symbol): GameResult[Game] =
    for
      _ <- GameError.NotPlayersTurn.raiseError.unlessA {
        player == nextPlayer
      }

      _ <- GameError.GameAlreadyFinished.raiseError.unlessA {
        winner.isEmpty
      }

      playerCardsNel <- getCardsFor(player)

      _ <- GameError.UnknownSymbolOnPlayersCard.raiseError.unlessA {
        playerCardsNel.head contains symbol
      }

      _ <- GameError.SymbolsNotMatching.raiseError.unlessA {
        cards.head contains symbol
      }

      nextPlayer <- playerAfter(player)
    yield Game(
      version = version + 1,
      cards = playerCardsNel.head :: cards,
      playerCards = playerCards.updateWith(player)(_ => playerCardsNel.tail),
      nextPlayer = nextPlayer
    )


  def winner: Option[Player] =
    playerCards.filter(_.isEmpty).keys.headOption


enum GameError:
  case UnknownPlayer
  case GameAlreadyFinished
  case NotPlayersTurn
  case UnknownSymbolOnPlayersCard
  case SymbolsNotMatching


type GameResult[T] = Either[GameError, T]
