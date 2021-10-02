package com.github.esgott.spottle.engine


import cats.Monoid
import cats.data.{NonEmptyList, NonEmptySet, StateT}
import cats.syntax.all._
import com.github.esgott.spottle.api.{Player, PublicGame, Symbol}
import com.github.esgott.spottle.api.kafka.v1.SpottleCommand._
import com.github.esgott.spottle.api.kafka.v1.SpottleEvent
import com.github.esgott.spottle.api.kafka.v1.SpottleEvent._
import com.github.esgott.spottle.core.{generateGame, Game}
import com.github.esgott.spottle.engine.GameStore.{GameEntry, GameMetadata}
import weaver._


object CommandHandlerTest extends SimpleIOSuite:
  case class State(store: Map[Long, GameEntry])


  object State:

    given Monoid[State] = Monoid.instance(
      State(Map.empty),
      (a, b) => State(a.store ++ b.store)
    )


  type E[A]    = Either[Throwable, A]
  type Test[A] = StateT[E, State, A]


  val gameStore = new GameStore[Test]:

    override def store(id: Long, game: Game, metadata: GameMetadata): Test[Unit] =
      StateT.modify(state => state |+| State(Map(id -> GameEntry(game, metadata))))


    override def drop(id: Long): Test[Unit] =
      StateT.modify(state => state.copy(store = state.store - id))


    override def get(id: Long): Test[Option[GameEntry]] =
      StateT.inspect(_.store.get(id))


  val commandHandler = new CommandHandler[Test](gameStore)

  val player1 = Player("creator")
  val player2 = Player("player")
  val players = NonEmptyList.of(player1, player2)

  val createCommand: CreateGame     = CreateGame(42, 2, player1, players)
  val getGameCommand: GetGame       = GetGame(42, player1)
  val guessCommand: Guess           = Guess(42, 0, player1, Symbol("0"))
  val finishGameCommand: FinishGame = FinishGame(42, player1)


  pureTest("new game is created") {
    val Right((finalState, events)) = commandHandler.handle(createCommand).runEmpty

    val GameUpdate(gameId, game, command) :: Nil = events

    expect(finalState.store.keySet == Set(42)) and
      expect(gameId == 42) and
      expect(command == createCommand) and
      expect(game.version == 0) and
      expect(game.playerCards.keys == NonEmptySet.of(player1, player2))
  }


  pureTest("get game") {
    val effect = for
      _      <- commandHandler.handle(createCommand)
      result <- commandHandler.handle(getGameCommand)
    yield result

    val Right(events) = effect.runEmptyA

    val GameUpdate(gameId, PublicGame(version, _, playerCards), command) :: Nil = events

    expect(gameId == 42) and
      expect(command == getGameCommand) and
      expect(version == 0) and
      expect(playerCards.keys == NonEmptySet.of(player1, player2))
  }


  pureTest("get non-existent game") {
    val Right(events) = commandHandler.handle(getGameCommand).runEmptyA

    val NotFound(gameId, message, command) :: Nil = events

    expect(message.contains("Game not found: 42")) and
      expect(gameId == getGameCommand.gameId) and
      expect(command == getGameCommand)
  }


  pureTest("guess correct symbol") {
    val effect = for
      createUpdate <- commandHandler.handle(createCommand)
      GameUpdate(_, PublicGame(_, card, playerCards), _) :: Nil = createUpdate
      commonSymbol = card intersect playerCards.lookup(player1).get.get
      result <- commandHandler.guess(guessCommand.copy(symbol = commonSymbol.head))
    yield result

    val Right(events) = effect.runEmptyA

    val GameUpdate(gameId, PublicGame(version, _, _), _) :: Nil = events

    expect(gameId == 42) and
      expect(version == 1)
  }


  pureTest("guess on non-existent game") {
    val Right(events) = commandHandler.guess(guessCommand).runEmptyA

    val NotFound(gameId, message, command) :: Nil = events

    expect(message.contains("Game not found: 42")) and
      expect(gameId == guessCommand.gameId) and
      expect(command == guessCommand)
  }


  pureTest("guess wrong symbol") {
    val effect = for
      createUpdate <- commandHandler.handle(createCommand)
      GameUpdate(_, game, _) :: Nil = createUpdate
      differentSymbol               = game.playerCards.lookup(player1).get.get diff game.card
      result <- commandHandler.guess(guessCommand.copy(symbol = differentSymbol.head))
    yield result

    val Right(events) = effect.runEmptyA

    val SymbolsNotMatching(gameId, _, _) :: Nil = events

    expect(gameId == guessCommand.gameId)
  }


  pureTest("guess symbol that does not exist on players card") {
    val effect = for
      createUpdate <- commandHandler.handle(createCommand)
      GameUpdate(_, game, _) :: Nil = createUpdate
      differentSymbol               = game.card diff game.playerCards.lookup(player1).get.get
      result <- commandHandler.guess(guessCommand.copy(symbol = differentSymbol.head))
    yield result

    val Right(events) = effect.runEmptyA

    val NotFound(gameId, message, _) :: Nil = events

    expect(message.contains("not present on players card")) and
      expect(gameId == guessCommand.gameId)
  }


  pureTest("guess on old game") {
    val effect = for
      createUpdate <- commandHandler.handle(createCommand)
      GameUpdate(_, PublicGame(_, card, playerCards), _) :: Nil = createUpdate
      commonSymbol = (card intersect playerCards.lookup(player1).get.get).head
      guessUpdate <- commandHandler.guess(guessCommand.copy(symbol = commonSymbol))
      GameUpdate(_, PublicGame(_, card2, playerCards2), _) :: Nil = guessUpdate
      commonSymbol2 = (card2 intersect playerCards2.lookup(player2).get.get).head
      result <- commandHandler.guess(guessCommand.copy(gameVersion = 0, symbol = commonSymbol2))
    yield result

    val Right(events) = effect.runEmptyA

    val GameHasAdvanced(gameId, version, newestVersion, _) :: Nil = events

    expect(gameId == guessCommand.gameId) and
      expect(newestVersion > version)
  }


  pureTest("winner announced") {
    def guess(event: SpottleEvent, player: Player) =
      val GameUpdate(_, PublicGame(version, card, playerCards), _) = event
      val commonSymbol = card intersect playerCards.lookup(player).get.get
      guessCommand.copy(gameVersion = version, player = player, symbol = commonSymbol.head)

    val effect = for {
      update1 <- commandHandler.handle(createCommand)
      update2 <- commandHandler.handle(guess(update1.head, player1))
      update3 <- commandHandler.handle(guess(update2.head, player2))
      update4 <- commandHandler.handle(guess(update3.head, player1))
      update5 <- commandHandler.handle(guess(update4.head, player2))
      result  <- commandHandler.handle(guess(update5.head, player1))
    } yield result

    val Right(events) = effect.runEmptyA

    val Winner(gameId, winner, PublicGame(version, _, playerCards)) :: Nil = events

    expect(gameId == 42) and
      expect(winner == player1) and
      expect(version == 5) and
      expect(playerCards.lookup(player1).get == None)
  }


  pureTest("game finished") {
    val Right(game)  = generateGame(2, CommandHandler.symbols.take(7).toSet, players, 42)
    val initialState = State(Map(42L -> GameEntry(game, GameMetadata(player1))))

    val Right((finalState, events)) = commandHandler.handle(finishGameCommand).run(initialState)

    expect(events.isEmpty) and
      expect(finalState.store.isEmpty)
  }
