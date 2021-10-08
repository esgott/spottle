package com.github.esgott.spottle.engine


import cats.effect.IO
import cats.syntax.all._
import ciris._
import com.github.esgott.spottle.kafka.{KafkaConfig, KafkaConsumerConfig, KafkaProducerConfig}


case class EngineConfig(
    port: Int,
    kafka: KafkaConfig,
    commands: KafkaConsumerConfig,
    events: KafkaProducerConfig
)


object EngineConfig:

  val config: ConfigValue[IO, EngineConfig] =
    (
      env("SERVICE_PORT").as[Int].default(8080),
      kafkaConfig,
      commandsConfig,
      eventsConfig
    ).parMapN(EngineConfig.apply)


  private val kafkaConfig: ConfigValue[IO, KafkaConfig] =
    (
      env("KAFKA_BOOTSTRAP_SERVER"),
      env("KAFKA_TRANSACTIONAL_ID").option
    ).parMapN(KafkaConfig.apply)


  private val commandsConfig: ConfigValue[IO, KafkaConsumerConfig] =
    (
      env("COMMANDS_TOPIC"),
      env("KAFKA_CONSUMER_GROUP").default("spottle-engine")
    ).parMapN(KafkaConsumerConfig.apply)


  private val eventsConfig: ConfigValue[IO, KafkaProducerConfig] =
    env("EVENTS_TOPIC").map(KafkaProducerConfig.apply)
