package com.github.esgott.spottle.edge


import com.github.esgott.spottle.kafka.{KafkaConfig, KafkaConsumerConfig, KafkaProducerConfig}
import io.circe.Codec


case class EdgeConfig(
    port: Int,
    kafka: KafkaConfig,
    commands: KafkaProducerConfig,
    events: KafkaConsumerConfig
) derives Codec.AsObject
