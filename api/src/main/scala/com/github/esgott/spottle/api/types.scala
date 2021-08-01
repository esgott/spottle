package com.github.esgott.spottle.api


import cats.Order
import cats.syntax.option._
import io.circe.{Decoder, Encoder, Json, KeyDecoder, KeyEncoder}
import sttp.tapir.{Codec, Schema}
import sttp.tapir.CodecFormat.TextPlain
import sttp.tapir.SchemaType.SString
import sttp.tapir.generic.auto._


// TODO migrate to circe-generic-extras when Scala 3 when https://github.com/circe/circe-generic-extras/issues/168 is solved

type Card = Set[Symbol]

opaque type Symbol = String


object Symbol:
  def apply(s: String): Symbol = s

  given Encoder[Symbol] = Json.fromString(_)

  given Decoder[Symbol] = _.as[String].map(Symbol.apply)

  given Schema[Symbol] = Schema(SString())


opaque type Player = String


object Player:

  def apply(s: String): Player = s


  given Order[Player] = Order.from[Player] { (a, b) =>
    if (a eq b) 0 else a.compareTo(b)
  }


  given KeyEncoder[Player] = { player => player }

  given KeyDecoder[Player] = _.some

  given Encoder[Player] = Json.fromString(_)

  given Decoder[Player] = _.as[String].map(Player.apply)

  given Schema[Player] = Schema(SString())


  given Codec[String, Player, TextPlain] =
    Codec.string.map(Player.apply)(identity).schema(summon[Schema[Player]])
