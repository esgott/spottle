package com.github.esgott.spottle.kafka


import cats.effect.IO
import com.github.esgott.spottle.kafka.KafkaProducerConfig._
import fs2.Stream
import fs2.kafka._
import io.circe.{Codec, Encoder}
import io.circe.generic.semiauto.deriveCodec
import io.circe.syntax._

import java.nio.charset.StandardCharsets
import scala.concurrent.duration._


case class KafkaProducerConfig(
    topicName: String,
    transactionalId: String
):

  def producerSettings(
      kafkaConfig: KafkaConfig
  ): TransactionalProducerSettings[IO, Array[Byte], Array[Byte]] =
    TransactionalProducerSettings(
      transactionalId,
      ProducerSettings[IO, Array[Byte], Array[Byte]]
        .withBootstrapServers(kafkaConfig.bootstrapServer)
    )


  def stream[K: Encoder, V: Encoder](kafkaConfig: KafkaConfig)(
      stream: Stream[IO, KafkaProducerRecord[K, V]]
  ): Stream[IO, Result[K, V]] =
    TransactionalKafkaProducer[IO].stream(producerSettings(kafkaConfig)).flatMap { producer =>
      stream
        .groupWithin(500, 1.seconds)
        .map { records =>
          TransactionalProducerRecords(records.map(serialize[K, V](topicName)), records.toList)
        }
        .evalMap(producer.produce)
        .map(_.passthrough)
    }


object KafkaProducerConfig:
  given Codec[KafkaProducerConfig] = deriveCodec

  case class KafkaProducerRecord[K, V](records: List[(K, V)], offset: CommittableOffset[IO])

  type Result[K, V] = List[KafkaProducerRecord[K, V]]


  def serialize[T: Encoder](value: T): Array[Byte] =
    value.asJson.noSpaces.getBytes(StandardCharsets.UTF_8)


  def serialize[K: Encoder, V: Encoder](topic: String)(
      record: KafkaProducerRecord[K, V]
  ): CommittableProducerRecords[IO, Array[Byte], Array[Byte]] =
    CommittableProducerRecords(
      records = record.records.map { case (key, value) =>
        ProducerRecord(topic, serialize(key), serialize(value))
      },
      offset = record.offset
    )
