package com.github.esgott.spottle.engine


import cats.MonadError
import cats.effect.kernel.Clock
import cats.instances.either._
import cats.syntax.all._
import com.github.esgott.spottle.api.{Player, PublicGame, Symbol}
import com.github.esgott.spottle.api.kafka.v1.{SpottleCommand, SpottleEvent}
import com.github.esgott.spottle.api.kafka.v1.SpottleCommand._
import com.github.esgott.spottle.api.kafka.v1.SpottleEvent._
import com.github.esgott.spottle.core.{generateGame, GenerationError}
import com.github.esgott.spottle.core.{FanoPlane, Game, GameError}
import com.github.esgott.spottle.engine.CommandHandler.symbols
import com.github.esgott.spottle.engine.GameStore.{GameEntry, GameMetadata}


class CommandHandler[F[_]](store: GameStore[F])(using MonadError[F, Throwable]):

  def handle(command: SpottleCommand): F[List[SpottleEvent]] = command match
    case command: CreateGame => createGame(command)
    case command: GetGame    => getGame(command)
    case command: Guess      => guess(command)
    case command: FinishGame => finsihGame(command)


  def createGame(command: CreateGame): F[List[SpottleEvent]] =
    val CreateGame(gameId, order, creator, players) = command

    val symbolSize     = FanoPlane.size(order)
    val symbolsForGame = symbols.take(symbolSize).toSet
    val game           = generateGame(order, symbolsForGame, players, gameId)
    val metadata       = GameMetadata(creator)

    for _ <- game.traverse(g => store.store(gameId, g, metadata))
    yield game match
      case Right(g) =>
        List(GameUpdate(gameId, toPublicGame(g), command))
      case Left(GenerationError.SymbolSizeNotPrime) =>
        List(InternalError(s"Symbol size $symbolSize is not prime", command))
      case Left(GenerationError.SymbolSizeIncorrectForOrder(_, shoudBe)) =>
        List(
          InternalError(
            s"Symbol size $symbolSize is incorrect for order $order, should be $shoudBe",
            command
          )
        )


  private def toPublicGame(game: Game): PublicGame =
    PublicGame(
      version = game.version,
      card = game.cards.head,
      playerCards = game.playerCards.map(_.headOption)
    )


  def getGame(command: GetGame): F[List[SpottleEvent]] =
    val GetGame(gameId, player) = command
    for gameEntry <- store.get(gameId)
    yield gameEntry match
      case Some(GameEntry(game, _)) =>
        List(GameUpdate(gameId, toPublicGame(game), command))
      case None =>
        List(NotFound(gameId, s"Game not found: $gameId", command))


  def guess(command: Guess): F[List[SpottleEvent]] =
    for
      gameEntry <- store.get(command.gameId)

      events <- gameEntry match
        case Some(gameEntry) if gameEntry.game.version == command.gameVersion =>
          guessAndStore(command, gameEntry)
        case Some(gameEntry) =>
          val newestVersion = gameEntry.game.version
          List(GameHasAdvanced(command.gameId, command.gameVersion, newestVersion, command)).pure[F]
        case None =>
          List(NotFound(command.gameId, s"Game not found: ${command.gameId}", command)).pure[F]
    yield events


  private def guessAndStore(command: Guess, gameEntry: GameEntry): F[List[SpottleEvent]] =
    val Guess(gameId, _, player, symbol) = command
    val GameEntry(game, metadata)        = gameEntry
    val result                           = game.guess(player, symbol)
    result match
      case Right(updated) =>
        for _ <- store.store(gameId, updated, metadata)
        yield updated.winner match
          case Some(winner) =>
            List(Winner(gameId, winner, toPublicGame(updated)))
          case None =>
            List(GameUpdate(gameId, toPublicGame(updated), command))
      case Left(GameError.UnknownPlayer) =>
        List(NotFound(gameId, s"Unkonwn player: $player", command)).pure[F]
      case Left(GameError.GameAlreadyFinished) =>
        List(GameAlreadyFinished(gameId, command)).pure[F]
      case Left(GameError.NotPlayersTurn) =>
        List(NotPlayersTurn(gameId, player, command)).pure[F]
      case Left(GameError.UnknownSymbolOnPlayersCard) =>
        List(NotFound(gameId, s"Symbol '$symbol' not present on players card", command)).pure[F]
      case Left(GameError.SymbolsNotMatching) =>
        List(SymbolsNotMatching(gameId, symbol, command)).pure[F]


  def finsihGame(command: FinishGame): F[List[SpottleEvent]] =
    for _ <- store.drop(command.gameId) yield List.empty


object CommandHandler:
  val symbols = LazyList.from(1).map(_.toString).map(Symbol.apply)
