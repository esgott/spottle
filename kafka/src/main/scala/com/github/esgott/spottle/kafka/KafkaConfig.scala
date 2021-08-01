package com.github.esgott.spottle.kafka


import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec


case class KafkaConfig(
    bootstrapServer: String,
    transactionalId: Option[String] = None
)


object KafkaConfig:
  given Codec[KafkaConfig] = deriveCodec
