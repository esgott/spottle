package com.github.esgott.spottle.api


import cats.Order
import cats.syntax.all._
import io.circe.{Codec => CirceCodec, Decoder, Encoder, Json, KeyDecoder, KeyEncoder}
import sttp.tapir.{Codec => TapirCodec, Schema}
import sttp.tapir.CodecFormat.TextPlain
import sttp.tapir.SchemaType.SString
import sttp.tapir.generic.auto._


// TODO migrate to codec derivation when https://github.com/circe/circe/issues/1777 is solved

type Card = Set[Symbol]

case class Symbol(value: String) extends AnyVal


object Symbol:

  given Encoder[Symbol] = Encoder[String].contramap(_.value)

  given Decoder[Symbol] = Decoder[String].map(Symbol.apply)


  given TapirCodec[String, Symbol, TextPlain] =
    TapirCodec.string.map(Symbol.apply)(_.value)


  given Schema[Symbol] = Schema.string[Symbol]


case class Player(value: String) extends AnyVal


object Player:

  given Order[Player] = Order.from[Player] { (a, b) =>
    if (a == b) 0 else a.value.compareTo(b.value)
  }


  given KeyEncoder[Player] = _.value

  given KeyDecoder[Player] = { player => Player(player).some }

  given Encoder[Player] = Encoder[String].contramap(_.value)

  given Decoder[Player] = Decoder[String].map(Player.apply)

  given Schema[Player] = Schema.string[Player]


  given TapirCodec[String, Player, TextPlain] =
    TapirCodec.string.map(Player.apply)(_.value)
