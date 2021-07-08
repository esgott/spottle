package com.github.esgott.spottle.core


import cats.data.NonEmptyList
import cats.syntax.either._
import com.github.esgott.spottle.api.{Player, Symbol}
import weaver._


object GeneratorTest extends SimpleIOSuite:

  def symbols(nrOfSymbols: Int): Set[Symbol] =
    (1 to nrOfSymbols).map(_.toString).map(Symbol.apply).toSet

  pureTest("incorrect number of symbols") {
    expect(
      generateGame(2, symbols(2), NonEmptyList.one(Player("p")), 1) ==
        GenerationError.SymbolSizeIncorrectForOrder(2, 7).asLeft
    )
  }


  pureTest("not prime order") {
    expect(
      generateGame(4, symbols(21), NonEmptyList.one(Player("p")), 1) ==
        GenerationError.SymbolSizeNotPrime.asLeft
    )
  }


  pureTest("order=2") {
    val Right(game) = generateGame(2, symbols(7), NonEmptyList.one(Player("p")), 1)

    forEach(game.cards) { card =>
      val restOfTheCards = game.cards.filter(_ != card)
      forEach(restOfTheCards) { otherCard =>
        expect(card.exists(otherCard.contains))
      }
    }
  }

  pureTest("games generated with same seeds equals") {
    val Right(game1) = generateGame(2, symbols(7), NonEmptyList.one(Player("p")), 1)
    val Right(game2) = generateGame(2, symbols(7), NonEmptyList.one(Player("p")), 1)
    val Right(game3) = generateGame(2, symbols(7), NonEmptyList.one(Player("p")), 2)
    expect(game1 == game2) and expect(game2 != game3)
  }
