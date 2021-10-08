package com.github.esgott.spottle.engine


import com.github.esgott.spottle.kafka.{KafkaConfig, KafkaConsumerConfig, KafkaProducerConfig}
import io.circe.Codec


case class EngineConfig(
    port: Int,
    kafka: KafkaConfig,
    commands: KafkaConsumerConfig,
    events: KafkaProducerConfig
) derives Codec.AsObject
