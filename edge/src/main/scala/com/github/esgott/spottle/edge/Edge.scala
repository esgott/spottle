package com.github.esgott.spottle.edge


import cats.effect.{ExitCode, IO, IOApp, Ref, Resource}
import cats.effect.std.Queue
import cats.syntax.all._
import com.github.esgott.spottle.api.kafka.v1.{SpottleCommand, SpottleEvent}
import com.github.esgott.spottle.kafka.{KafkaConfig, KafkaConsumerConfig, KafkaProducerConfig}
import com.github.esgott.spottle.service.Endpoints
import fs2.Stream
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.util.Random


object Edge extends IOApp:

  val resources =
    for
      config              <- Resource.eval[IO, EdgeConfig](EdgeConfig.config.load)
      commandProducerPipe <- config.commands.stream[Long, SpottleCommand](config.kafka)
      eventConsumerStream <- config.events.stream[Long, SpottleEvent](config.kafka)
    yield (config, commandProducerPipe, eventConsumerStream)


  override def run(args: List[String]): IO[ExitCode] =
    resources.use { case (config, commandProducerPipe, eventConsumerStream) =>
      val streamWithoutCommiting = eventConsumerStream.map(r => r.key -> r.value)

      for
        ready  <- Ref.of[IO, Boolean](false)
        logger <- Slf4jLogger.create[IO]
        kafka  <- EdgeKafka.edgeKafka[IO](streamWithoutCommiting, commandProducerPipe)
        server  = httpEndpoint(kafka, ready, config)
        streams = List(kafka.stream, server.serve)
        _ <- ready.set(true)
        _ <- logger.info("Service started")
        _ <- Stream.emits(streams).parJoin(streams.size).compile.drain
      yield ExitCode.Success
    }


  private def httpEndpoint(kafka: EdgeKafka[IO], ready: Ref[IO, Boolean], config: EdgeConfig) = {
    given EdgeKafka[IO]       = kafka
    given GameIdGenerator[IO] = GameIdGenerator.gameIdGenerator[IO](Random().nextLong)
    given EdgeHttp[IO]        = EdgeHttp.edgeHttp[IO]
    val endpoints             = EdgeEndpoints.edgeEndpoints[IO](ready)
    val logger                = Slf4jLogger.getLoggerFromName[IO]("com.github.esgott.edge.http")
    Endpoints.server(config.port, endpoints.routes, ready, logger)
  }
