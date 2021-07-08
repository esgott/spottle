package com.github.esgott.spottle.core


import cats.data.NonEmptyList
import cats.syntax.either._
import weaver._


object GeneratorTest extends SimpleIOSuite {

  pureTest("incorrect number of symbols") {
    expect(
      generateGame(2, Set(Symbol("a"), Symbol("b")), NonEmptyList.one(Player("p"))) ==
        GenerationError.SymbolSizeIncorrectForOrder(2, 7).asLeft
    )
  }


  pureTest("not prime order") {
    val symbols = (1 to 21).map(_.toString).map(Symbol.apply).toSet
    expect(
      generateGame(4, symbols, NonEmptyList.one(Player("p"))) ==
        GenerationError.SymbolSizeNotPrime.asLeft
    )
  }


  pureTest("order=2") {
    val symbols     = (1 to 7).map(_.toString).map(Symbol.apply).toSet
    val Right(game) = generateGame(2, symbols, NonEmptyList.one(Player("p")))

    forEach(game.cards) { card =>
      val restOfTheCards = game.cards.filter(_ != card)
      forEach(restOfTheCards) { otherCard =>
        expect(card.exists(otherCard.contains))
      }
    }
  }

}
