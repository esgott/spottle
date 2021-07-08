package com.github.esgott.spottle.core


import cats.data.NonEmptyList
import cats.syntax.all._
import weaver._


object GameTest extends SimpleIOSuite {

  val symbols            = (1 to 7).map(_.toString).map(Symbol.apply).toSet
  val playerA            = Player("A")
  val playerB            = Player("B")
  val players            = NonEmptyList.of(playerA, playerB)
  val Right(initialGame) = generateGame(2, symbols, players)


  def topCardForPlayer(game: Game, player: Player): Card =
    game.playerCards.lookup(player).get.head


  pureTest("initial game should not have winner") {
    expect(initialGame.winner == None)
  }


  pureTest("players follow each other") {
    expect(initialGame.playerAfter(playerA) == playerB.asRight) and
      expect(initialGame.playerAfter(playerB) == playerA.asRight)
  }


  pureTest("next player updated after a guess") {
    val commonSymbol = initialGame.cards.head intersect topCardForPlayer(initialGame, playerA)
    val Right(game)  = initialGame.guess(playerA, commonSymbol.head)
    expect(game.nextPlayer == playerB)
  }


  pureTest("player with more correct guesses wins") {
    // There are 3-3 cards for player A and B, game will finish after 5 correct guesses
    val finalGame = (1 to 5).foldLeft(initialGame) { (game, _) =>
      val player             = game.nextPlayer
      val commonSymbol       = game.cards.head intersect topCardForPlayer(game, player)
      val Right(updatedGame) = game.guess(player, commonSymbol.head)
      updatedGame
    }

    expect(finalGame.playerCards.lookup(playerA).get.size == 0) and
      expect(finalGame.playerCards.lookup(playerB).get.size == 1) and
      expect(finalGame.winner == playerA.some)
  }


  pureTest("error for unknown player") {
    expect(initialGame.playerAfter(Player("C")) == GameError.UnknownPlayer.asLeft)
  }


  pureTest("error for symbol missing on players card") {
    val missingSymbols = symbols diff topCardForPlayer(initialGame, playerA)
    expect(
      initialGame.guess(playerA, missingSymbols.head) == GameError.UnknownSymbolOnPlayersCard.asLeft
    )
  }


  pureTest("error for symbol missing on deck") {
    val missingSymbols = topCardForPlayer(initialGame, playerA) diff initialGame.cards.head
    expect(initialGame.guess(playerA, missingSymbols.head) == GameError.SymbolsNotMatching.asLeft)
  }


  pureTest("error for guess in a game that already finished") {
    // There are 3-3 cards for player A and B, game will finish after 5 correct guesses
    val finalGame = (1 to 5).foldLeft(initialGame) { (game, _) =>
      val player             = game.nextPlayer
      val commonSymbol       = game.cards.head intersect topCardForPlayer(game, player)
      val Right(updatedGame) = game.guess(player, commonSymbol.head)
      updatedGame
    }

    val commonSymbol = finalGame.cards.head intersect topCardForPlayer(finalGame, playerB)
    expect(finalGame.guess(playerB, commonSymbol.head) == GameError.GameAlreadyFinished.asLeft)
  }


  pureTest("error for player not in turn guess") {
    expect(initialGame.guess(playerB, symbols.head) == GameError.NotPlayersTurn.asLeft)
  }

}
