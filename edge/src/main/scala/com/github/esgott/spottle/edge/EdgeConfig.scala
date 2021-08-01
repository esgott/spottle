package com.github.esgott.spottle.edge


import com.github.esgott.spottle.kafka.{KafkaConfig, KafkaConsumerConfig, KafkaProducerConfig}
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec


case class EdgeConfig(
    port: Int,
    kafka: KafkaConfig,
    commands: KafkaProducerConfig,
    events: KafkaConsumerConfig
)


object EdgeConfig:
  given Codec[EdgeConfig] = deriveCodec
