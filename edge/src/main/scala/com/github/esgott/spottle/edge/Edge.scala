package com.github.esgott.spottle.edge


import cats.effect.{ExitCode, IO, IOApp}
import cats.effect.std.Queue
import cats.syntax.all._
import com.github.esgott.spottle.kafka.{KafkaConfig, KafkaConsumerConfig, KafkaProducerConfig}
import com.github.esgott.spottle.api.kafka.v1.{SpottleCommand, SpottleEvent}
import org.http4s.blaze.server._
import org.http4s.server.Router
import fs2.Stream

import scala.concurrent.ExecutionContext
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
        kafka <- EdgeKafka.edgeKafka[IO](streamWithoutCommiting, commandProducerPipe)
        given EdgeKafka[IO]       = kafka
        given GameIdGenerator[IO] = GameIdGenerator.gameIdGenerator[IO](Random().nextLong)
        given EdgeHttp[IO]        = EdgeHttp.edgeHttp[IO]
        endpoints                 = EdgeEndpoints.edgeEndpoints[IO]

        server = BlazeServerBuilder[IO](ExecutionContext.global)
          .bindHttp(config.port)
          .withHttpApp(Router("/" -> endpoints.routes).orNotFound)

        streams = List(kafka.stream, server.stream)
        _ <- Stream.emits(streams).parJoin(streams.size).compile.drain
      yield ExitCode.Success
    }
