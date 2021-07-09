package com.github.esgott.spottle.kafka


import cats.effect.IO
import com.github.esgott.spottle.kafka.KafkaConsumerConfig._
import fs2.Stream
import fs2.kafka._
import io.circe.{Codec, Decoder}
import io.circe.fs2._
import io.circe.generic.semiauto.deriveCodec


case class KafkaConsumerConfig(
    topicName: String,
    groupId: String
):

  def consumerSettings(kafkaConfig: KafkaConfig): ConsumerSettings[IO, Array[Byte], Array[Byte]] =
    ConsumerSettings[IO, Array[Byte], Array[Byte]]
      .withAutoOffsetReset(AutoOffsetReset.Earliest)
      .withIsolationLevel(IsolationLevel.ReadCommitted)
      .withBootstrapServers(kafkaConfig.bootstrapServer)
      .withGroupId(groupId)


  def stream[K: Decoder, V: Decoder](kafkaConfig: KafkaConfig): Stream[IO, KafkaConsumerRecord[K, V]] =
    KafkaConsumer
      .stream(consumerSettings(kafkaConfig))
      .evalTap(_.subscribeTo(topicName))
      .flatMap(_.stream)
      .flatMap(deserializeRecord[K, V])


object KafkaConsumerConfig:
  given Codec[KafkaConsumerConfig] = deriveCodec

  case class KafkaConsumerRecord[K, V](key: K, value: V, offset: CommittableOffset[IO])


  def deserialize[T: Decoder](value: Array[Byte]): Stream[IO, T] =
    Stream
      .emits(value)
      .through(byteArrayParser[IO])
      .through(decoder[IO, T])


  def deserializeRecord[K: Decoder, V: Decoder](
      record: CommittableConsumerRecord[IO, Array[Byte], Array[Byte]]
  ): Stream[IO, KafkaConsumerRecord[K, V]] =
    deserialize[K](record.record.key)
      .parZip(deserialize[V](record.record.value))
      .parZip(Stream.emit(record.offset))
      .map { case ((key, value), offset) => KafkaConsumerRecord(key, value, offset) }
