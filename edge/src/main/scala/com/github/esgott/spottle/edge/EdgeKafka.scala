package com.github.esgott.spottle.edge


import cats.effect.Concurrent
import cats.effect.std.Queue
import cats.effect.kernel.{Deferred, Fiber, Ref}
import cats.syntax.all._
import com.github.esgott.spottle.api.kafka.v1.{SpottleCommand, SpottleEvent}
import com.github.esgott.spottle.api.kafka.v1.SpottleEvent.GameUpdate
import fs2.{Pipe, Stream}


trait EdgeKafka[F[_]]:
  def send(command: SpottleCommand): F[SpottleEvent]
  def nextEvent(gameId: Long): F[SpottleEvent]
  def stream: Stream[F, List[(Long, SpottleCommand)]]


object EdgeKafka:

  case class Waiting[F[_]](
      predicate: PartialFunction[(Long, SpottleEvent), Boolean],
      deferred: Deferred[F, SpottleEvent]
  )


  def edgeKafka[F[_]: Concurrent](
      eventStream: Stream[F, (Long, SpottleEvent)],
      commmandProducerPipe: Pipe[F, (Long, SpottleCommand), List[(Long, SpottleCommand)]]
  ): F[EdgeKafka[F]] =
    for
      waiting      <- Ref.of[F, List[Waiting[F]]](List.empty)
      commandQueue <- Queue.unbounded[F, (Long, SpottleCommand)]
    yield new EdgeKafka[F]:

      override def send(command: SpottleCommand): F[SpottleEvent] =
        for
          deferred <- Deferred[F, SpottleEvent]

          waitForEvent = Waiting(
            { case (_, GameUpdate(_, _, commandRef)) => commandRef == command },
            deferred
          )

          _      <- waiting.update(waitForEvent :: _)
          result <- deferred.get
        yield result


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
