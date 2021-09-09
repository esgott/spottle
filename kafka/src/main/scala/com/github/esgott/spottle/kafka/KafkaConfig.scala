package com.github.esgott.spottle.kafka

import io.circe.Codec


case class KafkaConfig(
    bootstrapServer: String,
    transactionalId: Option[String] = None
) derives Codec.AsObject
