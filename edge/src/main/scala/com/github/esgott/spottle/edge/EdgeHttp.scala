package com.github.esgott.spottle.edge


import cats.MonadError
import cats.data.EitherT
import cats.effect.Sync
import cats.syntax.all._
import com.github.esgott.spottle.api.Player
import com.github.esgott.spottle.api.http.v1._
import com.github.esgott.spottle.api.kafka.v1.{SpottleCommand, SpottleEvent}
import org.typelevel.log4cats.slf4j.Slf4jLogger


trait EdgeHttp[F[_]]:
  import EdgeHttp._
  def createGame(request: CreateGame, player: Player): Result[F, GameUpdate]
  def getGame(gameId: Long, player: Player): Result[F, GameUpdate]
  def pollGame(gameId: Long, player: Player): Result[F, GameUpdate]
  def guess(request: Guess, player: Player): Result[F, GameUpdate]


object EdgeHttp:
  type Result[F[_], T] = EitherT[F, SpottleError, T]


  def edgeHttp[F[_]: Sync](using
      EdgeKafka[F],
      GameIdGenerator[F],
      MonadError[F, Throwable]
  ): F[EdgeHttp[F]] =
    for logger <- Slf4jLogger.create[F]
    yield new EdgeHttp[F]:

      override def createGame(request: CreateGame, player: Player): Result[F, GameUpdate] =
        for
          gameId <- EitherT.liftF(summon[GameIdGenerator[F]].nextGameid)
          _      <- EitherT.liftF(logger.info(s"Creating game with id $gameId"))
          allPlayers = player :: request.otherPlayers
          command    = SpottleCommand.CreateGame(gameId, request.order, player, allPlayers)
          event  <- EitherT.liftF(sendCommand(command))
          result <- expectGameUpdate(event)
        yield result


      private def sendCommand(command: SpottleCommand) =
        summon[EdgeKafka[F]].send(command)


      private def expectGameUpdate(event: SpottleEvent): Result[F, GameUpdate] = event match {
        case SpottleEvent.GameUpdate(gameId, game, _) =>
          EitherT.pure(GameUpdate(gameId, game))
        case SpottleEvent.Winner(gameId, _, game) =>
          EitherT.pure(GameUpdate(gameId, game))
        case SpottleEvent.NotFound(gameId, message, command) =>
          EitherT.leftT(SpottleError.NotFound(message))
        case SpottleEvent.GameHasAdvanced(gameId, version, newestVersion, command) =>
          EitherT.leftT(
            SpottleError.BadRequest(s"Game has already advanced to version $newestVersion")
          )
        case SpottleEvent.GameAlreadyFinished(gameId, command) =>
          EitherT.leftT(SpottleError.BadRequest("Game already finished"))
        case SpottleEvent.NotPlayersTurn(gameId, player, command) =>
          EitherT.leftT(SpottleError.BadRequest("Not players turn"))
        case SpottleEvent.SymbolsNotMatching(gameId, symbol, command) =>
          EitherT.leftT(SpottleError.BadRequest("Symbols not matching"))
        case SpottleEvent.InternalError(message, command) =>
          EitherT.liftF(new RuntimeException(s"Internal error: $message").raiseError)
      }


      override def getGame(gameId: Long, player: Player): Result[F, GameUpdate] =
        for
          event  <- EitherT.liftF(sendCommand(SpottleCommand.GetGame(gameId, player)))
          result <- expectGameUpdate(event)
        yield result


      override def pollGame(gameId: Long, player: Player): Result[F, GameUpdate] =
        for
          event    <- EitherT.liftF(nextEvent(gameId))
          response <- expectGameUpdate(event)
        yield response


      private def nextEvent(gameId: Long) =
        summon[EdgeKafka[F]].nextEvent(gameId)


      override def guess(request: Guess, player: Player): Result[F, GameUpdate] =
        val command =
          SpottleCommand.Guess(request.gameId, request.gameVersion, player, request.symbol)
        for
          event    <- EitherT.liftF(sendCommand(command))
          response <- expectGameUpdate(event)
        yield response
