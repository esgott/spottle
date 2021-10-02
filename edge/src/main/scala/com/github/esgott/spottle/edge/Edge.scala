package com.github.esgott.spottle.edge


import cats.effect.{ExitCode, IO, IOApp}
import cats.effect.std.Queue
import com.github.esgott.spottle.kafka.{KafkaConfig, KafkaConsumerConfig, KafkaProducerConfig}
import com.github.esgott.spottle.api.kafka.v1.{SpottleCommand, SpottleEvent}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import fs2.Stream


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

        given EdgeKafka[IO] = kafka

        _ = summon[EdgeKafka[IO]]

        http  = EdgeHttp.edgeHttp[IO]

        endpoints           = EdgeEndpoints.edgeEndpoints[IO]

        server <- BlazeServerBuilder[IO]
          .bindHttp(config.port)
          .withHttpApp(Router("/" -> endpoints.routes).orNotFound)

        streams = List(summon[EdgeKafka[IO]].stream, server.stream)
        _ <- Stream.emits(streams).parJoin(streams.size)
      yield ExitCode.Success
    }
