package com.github.esgott.spottle.engine


import cats.effect.{ExitCode, IO, IOApp}
import com.github.esgott.spottle.api.kafka.v1.{SpottleCommand, SpottleEvent}
import com.github.esgott.spottle.kafka.KafkaConsumerConfig.KafkaConsumerRecord
import com.github.esgott.spottle.kafka.{KafkaConfig, KafkaConsumerConfig, KafkaProducerConfig}
import com.github.esgott.spottle.kafka.KafkaProducerConfig.KafkaProducerRecord


object Engine extends IOApp:

  private val config =
    EngineConfig(
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
      commandConsumerStream <- config.commands.stream[Long, SpottleCommand](config.kafka)
      eventProducerStream   <- config.events.transactionalStream[Long, SpottleEvent](config.kafka)
    yield (commandConsumerStream, eventProducerStream)


  override def run(args: List[String]): IO[ExitCode] =
    resources.use { case (commandConsumerStream, eventProducerStream) =>
      for
        gameStore <- GameStore()
        commandHandler = new CommandHandler[IO](gameStore)

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
