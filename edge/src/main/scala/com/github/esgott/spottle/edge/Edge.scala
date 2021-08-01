package com.github.esgott.spottle.edge


import com.github.esgott.spottle.kafka.{KafkaConfig, KafkaConsumerConfig, KafkaProducerConfig}
import cats.effect.{ExitCode, IO, IOApp}
import com.github.esgott.spottle.api.kafka.v1.{SpottleCommand, SpottleEvent}


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
      commandProducerStream <- config.commands.stream[Long, SpottleCommand](config.kafka)
      eventConsumerStream   <- config.events.stream[Long, SpottleEvent](config.kafka)
    yield (commandProducerStream, eventConsumerStream)


  override def run(args: List[String]): IO[ExitCode] =
    resources.use { case (commandProducerStream, eventConsumerStream) =>
      IO(ExitCode.Success)
    }
