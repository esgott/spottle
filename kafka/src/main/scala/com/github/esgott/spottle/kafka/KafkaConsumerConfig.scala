package com.github.esgott.spottle.kafka


import cats.effect.{IO, Resource}
import com.github.esgott.spottle.kafka.KafkaConsumerConfig._
import fs2.Stream
import fs2.kafka._
import io.circe.{Codec, Decoder}
import io.circe.fs2._


case class KafkaConsumerConfig(
    topicName: String,
    groupId: String
) derives Codec.AsObject:

  def consumerSettings(kafkaConfig: KafkaConfig): ConsumerSettings[IO, Array[Byte], Array[Byte]] =
    ConsumerSettings[IO, Array[Byte], Array[Byte]]
      .withAutoOffsetReset(AutoOffsetReset.Earliest) // TODO from config
      .withIsolationLevel(IsolationLevel.ReadCommitted)
      .withBootstrapServers(kafkaConfig.bootstrapServer)
      .withGroupId(groupId)


  def stream[K: Decoder, V: Decoder](
      kafkaConfig: KafkaConfig
  ): Resource[IO, Stream[IO, KafkaConsumerRecord[K, V]]] =
    for
      consumer <- KafkaConsumer.resource(consumerSettings(kafkaConfig))
      _        <- Resource.eval(consumer.subscribeTo(topicName))
    yield consumer.stream
      .flatMap(deserializeRecord[K, V])


object KafkaConsumerConfig:

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
