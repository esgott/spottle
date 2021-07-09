package com.github.esgott.spottle.engine


import com.github.esgott.spottle.kafka.{KafkaConfig, KafkaConsumerConfig, KafkaProducerConfig}
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec


case class EngineConfig(
    kafka: KafkaConfig,
    commands: KafkaConsumerConfig,
    events: KafkaProducerConfig
)


object EngineConfig:
  given Codec[EngineConfig] = deriveCodec
