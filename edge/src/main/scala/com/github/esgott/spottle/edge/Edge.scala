package com.github.esgott.spottle.edge


import cats.effect.{ExitCode, IO, IOApp, Ref}
import cats.effect.std.Queue
import cats.syntax.all._
import com.github.esgott.spottle.api.kafka.v1.{SpottleCommand, SpottleEvent}
import com.github.esgott.spottle.kafka.{KafkaConfig, KafkaConsumerConfig, KafkaProducerConfig}
import com.github.esgott.spottle.service.Endpoints
import fs2.Stream

import scala.util.Random


object Edge extends IOApp:

  private val config = EdgeConfig(
    port = 8080,
    kafka = KafkaConfig(
      bootstrapServer = "kafka:9092"
    ),
    commands = KafkaProducerConfig(
      topicName = "spottle.commands.v1"
    ),
    events = KafkaConsumerConfig(
      topicName = "spottle.events.v1",
      groupId = "TODO"
    )
  )


  val resources =
    for
      commandProducerPipe <- config.commands.stream[Long, SpottleCommand](config.kafka)
      eventConsumerStream <- config.events.stream[Long, SpottleEvent](config.kafka)
    yield (commandProducerPipe, eventConsumerStream)


  override def run(args: List[String]): IO[ExitCode] =
    resources.use { case (commandProducerPipe, eventConsumerStream) =>
      val streamWithoutCommiting = eventConsumerStream.map(r => r.key -> r.value)

      for
        ready <- Ref.of[IO, Boolean](false)
        kafka <- EdgeKafka.edgeKafka[IO](streamWithoutCommiting, commandProducerPipe)
        server  = httpEndpoint(kafka, ready)
        streams = List(kafka.stream, server.stream)
        _ <- ready.set(true)
        _ <- Stream.emits(streams).parJoin(streams.size).compile.drain
      yield ExitCode.Success
    }


  private def httpEndpoint(kafka: EdgeKafka[IO], ready: Ref[IO, Boolean]) = {
    given EdgeKafka[IO]       = kafka
    given GameIdGenerator[IO] = GameIdGenerator.gameIdGenerator[IO](Random().nextLong)
    given EdgeHttp[IO]        = EdgeHttp.edgeHttp[IO]
    val endpoints             = EdgeEndpoints.edgeEndpoints[IO](ready)
    Endpoints.server[IO](config.port, endpoints.routes)
  }
