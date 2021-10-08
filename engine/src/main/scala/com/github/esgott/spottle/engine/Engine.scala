package com.github.esgott.spottle.engine


import cats.effect.{ExitCode, IO, IOApp, Ref, Resource}
import com.github.esgott.spottle.api.kafka.v1.{SpottleCommand, SpottleEvent}
import com.github.esgott.spottle.kafka.KafkaConsumerConfig.KafkaConsumerRecord
import com.github.esgott.spottle.kafka.{KafkaConfig, KafkaConsumerConfig, KafkaProducerConfig}
import com.github.esgott.spottle.kafka.KafkaProducerConfig.KafkaProducerRecord
import com.github.esgott.spottle.service.Endpoints


object Engine extends IOApp:

  private val config =
    EngineConfig(
      port = 8080,
      kafka = KafkaConfig(
        bootstrapServer = "kafka:9092",
        transactionalId = Some("TODO")
      ),
      commands = KafkaConsumerConfig(
        topicName = "spottle.commands.v1",
        groupId = "TODO"
      ),
      events = KafkaProducerConfig(
        topicName = "spottle.events.v1"
      )
    )


  private val resources =
    for
      ready <- Resource.eval[IO, Ref[IO, Boolean]](Ref.of(false))

      commandConsumerStream <- config.commands.stream[Long, SpottleCommand](config.kafka)
      eventProducerStream   <- config.events.transactionalStream[Long, SpottleEvent](config.kafka)

      _ <- Endpoints.server[IO](config.port, EngineEndpoints.engineEndpoints(ready).routes).resource
    yield (ready, commandConsumerStream, eventProducerStream)


  override def run(args: List[String]): IO[ExitCode] =
    resources.use { case (ready, commandConsumerStream, eventProducerStream) =>
      for
        gameStore <- GameStore()
        commandHandler = new CommandHandler[IO](gameStore)
        _ <- ready.set(true)

        _ <- commandConsumerStream
          .evalMap { case KafkaConsumerRecord(key, command, offset) =>
            for
              events <- commandHandler.handle(command)
              records = events.map(key -> _)
            yield KafkaProducerRecord(records, offset)
          }
          .through(eventProducerStream)
          .compile
          .drain
      yield ExitCode.Success
    }
