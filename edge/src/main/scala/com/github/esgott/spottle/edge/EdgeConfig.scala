package com.github.esgott.spottle.edge


import cats.effect.IO
import cats.syntax.all._
import ciris._
import com.github.esgott.spottle.kafka.{KafkaConfig, KafkaConsumerConfig, KafkaProducerConfig}
import io.circe.Codec


case class EdgeConfig(
    port: Int,
    kafka: KafkaConfig,
    commands: KafkaProducerConfig,
    events: KafkaConsumerConfig
)


object EdgeConfig:

  val config: ConfigValue[IO, EdgeConfig] =
    (
      env("SERVICE_PORT").as[Int].default(8080),
      kafkaConfig,
      commandsConfig,
      eventssConfig
    ).parMapN(EdgeConfig.apply)


  private val kafkaConfig: ConfigValue[IO, KafkaConfig] =
    (
      env("KAFKA_BOOTSTRAP_SERVER"),
      env("KAFKA_TRANSACTIONAL_ID").option
    ).parMapN(KafkaConfig.apply)


  private val commandsConfig: ConfigValue[IO, KafkaProducerConfig] =
    env("COMMANDS_TOPIC").map(KafkaProducerConfig.apply)


  private val eventssConfig: ConfigValue[IO, KafkaConsumerConfig] =
    (
      env("EVENTS_TOPIC"),
      env("KAFKA_CONSUMER_GROUP").default("spottle-edge")
    ).parMapN(KafkaConsumerConfig.apply)
