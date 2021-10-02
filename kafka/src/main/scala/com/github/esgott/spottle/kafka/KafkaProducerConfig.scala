package com.github.esgott.spottle.kafka


import cats.effect.{IO, Resource}
import cats.syntax.all._
import com.github.esgott.spottle.kafka.KafkaProducerConfig._
import fs2.{Pipe, Stream}
import fs2.kafka._
import io.circe.{Codec, Encoder}
import io.circe.syntax._

import java.nio.charset.StandardCharsets
import scala.concurrent.duration._


case class KafkaProducerConfig(
    topicName: String
) derives Codec.AsObject:

  def transactionalProducerSettings(
      kafkaConfig: KafkaConfig
  ): IO[TransactionalProducerSettings[IO, Array[Byte], Array[Byte]]] =
    for
      transactionalId <- kafkaConfig.transactionalId.liftTo[IO] {
        new RuntimeException(
          "Transactional ID is required to create a transactional producer stream"
        )
      }
    yield TransactionalProducerSettings(
      transactionalId,
      producerSettings(kafkaConfig)
    )


  def producerSettings(kafkaConfig: KafkaConfig): ProducerSettings[IO, Array[Byte], Array[Byte]] =
    ProducerSettings[IO, Array[Byte], Array[Byte]]
      .withBootstrapServers(kafkaConfig.bootstrapServer)


  def transactionalStream[K: Encoder, V: Encoder](
      kafkaConfig: KafkaConfig
  ): Resource[IO, Pipe[IO, KafkaProducerRecord[K, V], List[KafkaProducerRecord[K, V]]]] =
    for
      producerSettings <- Resource.eval(transactionalProducerSettings(kafkaConfig))
      producer         <- TransactionalKafkaProducer[IO].resource(producerSettings)
    yield stream =>
      stream
        .groupWithin(500, 1.seconds)
        .map { records =>
          val serialized = records.map(serializeCommittableProducerRecords[K, V](topicName))
          TransactionalProducerRecords(serialized, records.toList)
        }
        .evalMap(producer.produce)
        .map(_.passthrough)


  def stream[K: Encoder, V: Encoder](
      kafkaConfig: KafkaConfig
  ): Resource[IO, Pipe[IO, (K, V), List[(K, V)]]] =
    for producer <- KafkaProducer.resource(producerSettings(kafkaConfig))
    yield stream =>
      stream
        .groupWithin(500, 1.seconds)
        .map { records =>
          val serialized = records.map(serializeProducerRecords[K, V](topicName))
          ProducerRecords(serialized, records.toList)
        }
        .evalMap(producer.produce)
        .evalMap(identity)
        .map(_.passthrough)


object KafkaProducerConfig:

  case class KafkaProducerRecord[K, V](records: List[(K, V)], offset: CommittableOffset[IO])


  def serialize[T: Encoder](value: T): Array[Byte] =
    value.asJson.noSpaces.getBytes(StandardCharsets.UTF_8)


  def serializeCommittableProducerRecords[K: Encoder, V: Encoder](topic: String)(
      record: KafkaProducerRecord[K, V]
  ): CommittableProducerRecords[IO, Array[Byte], Array[Byte]] =
    CommittableProducerRecords(
      records = record.records.map(serializeProducerRecords(topic)),
      offset = record.offset
    )


  def serializeProducerRecords[K: Encoder, V: Encoder](topic: String)(
      record: (K, V)
  ): ProducerRecord[Array[Byte], Array[Byte]] =
    record match {
      case (key, value) =>
        ProducerRecord(topic, serialize(key), serialize(value))
    }
