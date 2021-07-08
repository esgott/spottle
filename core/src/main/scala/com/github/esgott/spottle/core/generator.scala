package com.github.esgott.spottle.core


import cats.data.NonEmptyList
import cats.syntax.all._
import com.github.esgott.spottle.api.{Card, Player, Symbol}
import com.github.esgott.spottle.core.FanoPlane._

import scala.util.Random


enum GenerationError:
  case SymbolSizeNotPrime
  case SymbolSizeIncorrectForOrder(order: Int, sizeShouldBe: Int)
  case EmptyCards


type GenerationResult[T] = Either[GenerationError, T]


def generateGame(
    order: Int,
    symbols: Set[Symbol],
    players: NonEmptyList[Player],
    seed: Long
): GenerationResult[Game] =
  for
    _ <- isPrime(symbols.size)
    _ <- checkSymbolsCount(order, symbols)
  yield generateUnsafe(order, symbols, players, seed)


private def isPrime(n: Int) =
  if (n == 2)
    ().asRight
  else if (n < 2 || n % 2 == 0)
    GenerationError.SymbolSizeNotPrime.asLeft
  else
    Either.cond(
      LazyList
        .from(3, 2)
        .takeWhile(i => i * i < n + 1)
        .forall(i => n % i != 0),
      (),
      GenerationError.SymbolSizeNotPrime
    )


private def checkSymbolsCount(order: Int, symbols: Set[Symbol]) =
  Either.cond(
    symbols.size == FanoPlane.size(order),
    (),
    GenerationError.SymbolSizeIncorrectForOrder(order, FanoPlane.size(order))
  )


private def generateUnsafe(
    order: Int,
    symbols: Set[Symbol],
    players: NonEmptyList[Player],
    seed: Long
): Game =
  val fanoPlane = FanoPlane(order)

  val cards = for
    row <- 0 until fanoPlane.rows

    selectedSymbols = fanoPlane
      .row(row)
      .toScalaVector
      .zip(symbols)
      .collect { case (1, symbol) => symbol }
      .toSet
  yield selectedSymbols: Card

  val random          = Random(seed)
  val shuffledCards   = random.shuffle(cards.toList)
  val cardsToDeal     = shuffledCards.tail
  val nrOfCardsToDeal = cardsToDeal.size / players.size

  val playerCards = players.zipWithIndex.map { case (player, index) =>
    val playerCards = cardsToDeal.drop(nrOfCardsToDeal * index).take(nrOfCardsToDeal)
    player -> playerCards.toList
  }.toNem

  val cardsNotDealt = cardsToDeal.drop(players.size * nrOfCardsToDeal).toList

  Game(
    version = 0,
    cards = NonEmptyList.ofInitLast(cardsNotDealt, shuffledCards.head),
    playerCards = playerCards,
    nextPlayer = playerCards.keys.toNonEmptyList.head
  )
