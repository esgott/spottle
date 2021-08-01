package com.github.esgott.spottle.engine


import cats.effect.{ExitCode, IO, IOApp}
import com.github.esgott.spottle.api.kafka.v1.{SpottleCommand, SpottleEvent}
import com.github.esgott.spottle.kafka.KafkaConsumerConfig.KafkaConsumerRecord
import com.github.esgott.spottle.kafka.{KafkaConfig, KafkaConsumerConfig, KafkaProducerConfig}
import com.github.esgott.spottle.kafka.KafkaProducerConfig.{KafkaProducerRecord, Result}
import fs2.Stream


object Engine extends IOApp:

  private val config =
    EngineConfig(
      kafka = KafkaConfig(
        bootstrapServer = "kafka:9092"
      ),
      commands = KafkaConsumerConfig(
        topicName = "spottle.commands.v1",
        groupId = "TODO"
      ),
      events = KafkaProducerConfig(
        topicName = "spottle.events.v1",
        transactionalId = "TODO"
      )
    )


  override def run(args: List[String]): IO[ExitCode] =
    for
      gameStore <- GameStore.apply()
      commandHandler = new CommandHandler[IO](gameStore)
      _ <- stream(config, commandHandler).compile.drain
    yield ExitCode.Success


  def stream(
      config: EngineConfig,
      commandHandler: CommandHandler[IO]
  ): Stream[IO, Result[Long, SpottleEvent]] =
    config.commands
      .stream[Long, SpottleCommand](config.kafka)
      .evalMap { case KafkaConsumerRecord(key, command, offset) =>
        for
          events <- commandHandler.handle(command)
          records = events.map(key -> _)
        yield KafkaProducerRecord(records, offset)
      }
      .through(config.events.stream(config.kafka))
