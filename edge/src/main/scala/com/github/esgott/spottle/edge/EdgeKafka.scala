package com.github.esgott.spottle.edge


import cats.effect.Async
import cats.effect.std.Queue
import cats.effect.kernel.{Deferred, Fiber, Ref}
import cats.syntax.all._
import com.github.esgott.spottle.api.kafka.v1.{SpottleCommand, SpottleEvent}
import com.github.esgott.spottle.api.kafka.v1.SpottleEvent.GameUpdate
import fs2.{Pipe, Stream}
import org.typelevel.log4cats.slf4j.Slf4jLogger


trait EdgeKafka[F[_]]:
  def send(command: SpottleCommand): F[SpottleEvent]
  def nextEvent(gameId: Long): F[SpottleEvent]
  def stream: Stream[F, List[(Long, SpottleCommand)]]


object EdgeKafka:

  case class Waiting[F[_]](
      predicate: PartialFunction[(Long, SpottleEvent), Boolean],
      deferred: Deferred[F, SpottleEvent]
  )


  def edgeKafka[F[_]: Async](
      eventStream: Stream[F, (Long, SpottleEvent)],
      commmandProducerPipe: Pipe[F, (Long, SpottleCommand), List[(Long, SpottleCommand)]]
  ): F[EdgeKafka[F]] =
    for
      logger       <- Slf4jLogger.create[F]
      waiting      <- Ref.of[F, List[Waiting[F]]](List.empty)
      commandQueue <- Queue.unbounded[F, (Long, SpottleCommand)]
    yield new EdgeKafka[F]:

      override def send(command: SpottleCommand): F[SpottleEvent] =
        for
          _ <- commandQueue.offer(gameId(command) -> command)
          _ <- logger.debug(s"Sent $command")

          deferred <- Deferred[F, SpottleEvent]

          waitForEvent = Waiting(
            { case (_, GameUpdate(_, _, commandRef)) => commandRef == command },
            deferred
          )

          _      <- waiting.update(waitForEvent :: _)
          result <- deferred.get
        yield result


      private def gameId(command: SpottleCommand) = command match {
        case SpottleCommand.CreateGame(gameId, _, _, _) => gameId
        case SpottleCommand.GetGame(gameId, _)          => gameId
        case SpottleCommand.Guess(gameId, _, _, _)      => gameId
        case SpottleCommand.FinishGame(gameId, _)       => gameId
      }


      override def nextEvent(gameId: Long): F[SpottleEvent] =
        for
          deferred <- Deferred[F, SpottleEvent]

          waitForEvent = Waiting(
            { case (eventGameId, _) => eventGameId == gameId },
            deferred
          )

          _      <- waiting.update(waitForEvent :: _)
          result <- deferred.get
        yield result


      override def stream: Stream[F, List[(Long, SpottleCommand)]] =
        val eventProcessing = eventStream
          .evalTap(event => logger.debug(s"Received $event"))
          .evalMap(finishWaiting)

        val commandProcessing = Stream
          .fromQueueUnterminated(commandQueue)
          .through(commmandProducerPipe)

        commandProcessing.concurrently(eventProcessing)


      private def finishWaiting(event: (Long, SpottleEvent)): F[Unit] =
        for
          waitings <- waiting.get

          _ <- waitings
            .filter(_.predicate.isDefinedAt(event))
            .filter(_.predicate(event))
            .traverse(_.deferred.complete(event._2))
        yield ()
