package com.github.esgott.spottle.core


import cats.syntax.either._
import com.github.esgott.spottle.core.FanoPlane._


enum GenerationError:
  case SymbolSizeNotPrime
  case SymbolSizeIncorrectForOrder(order: Int, sizeShouldBe: Int)


def generateGame(
    order: Int,
    symbols: Set[Symbol],
    players: List[Player]
): Either[GenerationError, Game] =
  for
    _ <- isPrime(symbols.size)
    _ <- checkSymbolsCount(order, symbols)
  yield generateUnsafe(order, symbols, players)


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


private def generateUnsafe(order: Int, symbols: Set[Symbol], players: List[Player]): Game =
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

  Game(
    version = 0,
    cards = cards.toList,
    players = players,
    playerCards = players.map(_ -> List.empty).toMap,
    players.head
  )
