package com.github.esgott.spottle.edge


import cats.data.{NonEmptyList, NonEmptyMap}
import cats.effect.IO
import cats.effect.std.Queue
import cats.syntax.all.*
import com.github.esgott.spottle.api.http.v1.{CreateGame, GameUpdate, Guess, SpottleError}
import com.github.esgott.spottle.api.{Player, PublicGame, Symbol}
import com.github.esgott.spottle.api.kafka.v1.{SpottleCommand, SpottleEvent}
import fs2.Stream
import weaver.*


object EdgeHttpTest extends SimpleIOSuite:

  case class InMemoryKafka(
      sentMessages: Queue[IO, SpottleCommand],
      responses: Queue[IO, SpottleEvent]
  ) extends EdgeKafka[IO]:
    override def send(command: SpottleCommand): IO[SpottleEvent]  = offer(command) >> take
    override def nextEvent(gameId: Long): IO[SpottleEvent]        = take
    override def stream: Stream[IO, List[(Long, SpottleCommand)]] = Stream.empty
    def sent: IO[Option[SpottleCommand]]                          = sentMessages.tryTake
    private def offer(command: SpottleCommand)                    = sentMessages.offer(command)
    private def take                                              = responses.take
    def addResponse(response: SpottleEvent): IO[Unit]             = responses.offer(response)


  object InMemoryKafka:

    def apply(): IO[InMemoryKafka] =
      for
        sentMessages <- Queue.unbounded[IO, SpottleCommand]
        responses    <- Queue.unbounded[IO, SpottleEvent]
      yield InMemoryKafka(sentMessages, responses)


  private val gameId      = 0
  private val player      = Player("player")
  private val otherPlayer = Player("other")
  private val symbol      = Symbol("s1")
  private val card        = Set(symbol)


  given GameIdGenerator[IO] with
    override def nextGameid: IO[Long] = IO(gameId)


  test("create game") {
    for
      kafka <- InMemoryKafka()
      given EdgeKafka[IO] = kafka

      order       = 2
      publicGame  = PublicGame(gameId, card, Map(otherPlayer -> None))
      players     = NonEmptyList.of(player, otherPlayer)
      kafkaCreate = SpottleCommand.CreateGame(gameId, order, player, players)

      _ <- kafka.addResponse(SpottleEvent.GameUpdate(0, publicGame, kafkaCreate))

      http = EdgeHttp.edgeHttp[IO]

      request = CreateGame(order, NonEmptyList.of(otherPlayer))
      response <- http.createGame(request, player).value
      sent     <- kafka.sent
    yield expect(response == GameUpdate(gameId, publicGame).asRight) and
      expect(sent == kafkaCreate.some)
  }


  test("client error") {
    for
      kafka <- InMemoryKafka()
      given EdgeKafka[IO] = kafka

      getGame = SpottleCommand.GetGame(gameId, player)

      _ <- kafka.addResponse(SpottleEvent.NotFound(gameId, "Something not found", getGame))

      http = EdgeHttp.edgeHttp[IO]

      response <- http.getGame(gameId, player).value
    yield expect(response == SpottleError.NotFound("Something not found").asLeft)
  }


  test("internal error") {
    for
      kafka <- InMemoryKafka()
      given EdgeKafka[IO] = kafka

      getGame = SpottleCommand.GetGame(gameId, player)

      _ <- kafka.addResponse(SpottleEvent.InternalError("something went wrong", getGame))

      http = EdgeHttp.edgeHttp[IO]

      response <- http.getGame(gameId, player).value.attempt
      Left(error) = response
    yield expect(error.getMessage == "Internal error: something went wrong")
  }


  test("get game") {
    for
      kafka <- InMemoryKafka()
      given EdgeKafka[IO] = kafka

      getGame    = SpottleCommand.GetGame(gameId, player)
      publicGame = PublicGame(gameId, card, Map(otherPlayer -> None))

      _ <- kafka.addResponse(SpottleEvent.GameUpdate(gameId, publicGame, getGame))

      http = EdgeHttp.edgeHttp[IO]

      response <- http.getGame(gameId, player).value
      sent     <- kafka.sent
    yield expect(response == GameUpdate(gameId, publicGame).asRight) and
      expect(sent == getGame.some)
  }


  test("poll game") {
    for
      kafka <- InMemoryKafka()
      given EdgeKafka[IO] = kafka

      getGame    = SpottleCommand.GetGame(gameId, player)
      publicGame = PublicGame(gameId, card, Map(otherPlayer -> None))

      _ <- kafka.addResponse(SpottleEvent.GameUpdate(gameId, publicGame, getGame))

      http = EdgeHttp.edgeHttp[IO]

      response <- http.pollGame(gameId, player).value
      sent     <- kafka.sent
    yield expect(response == GameUpdate(gameId, publicGame).asRight) and
      expect(sent == None)
  }


  test("guess") {
    for
      kafka <- InMemoryKafka()
      given EdgeKafka[IO] = kafka

      publicGame = PublicGame(gameId, card, Map(otherPlayer -> None))
      _ <- kafka.addResponse(SpottleEvent.Winner(gameId, player, publicGame))

      http = EdgeHttp.edgeHttp[IO]

      request = Guess(gameId, 0, symbol)
      response <- http.guess(request, player).value
      sent     <- kafka.sent
    yield expect(response == GameUpdate(gameId, publicGame).asRight) and
      expect(sent == SpottleCommand.Guess(gameId, 0, player, symbol).some)
  }
