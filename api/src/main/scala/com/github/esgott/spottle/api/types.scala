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

opaque type Symbol = String


object Symbol:
  def apply(s: String): Symbol = s


  given CirceCodec[Symbol] =
    CirceCodec.from(Decoder[String], Encoder[String]).imap(Symbol.apply)(identity)


  given TapirCodec[String, Symbol, TextPlain] =
    TapirCodec.string.map(Symbol.apply)(identity)


  given Schema[Symbol] = Schema.string[Symbol]


opaque type Player = String


object Player:

  def apply(s: String): Player = s


  given Order[Player] = Order.from[Player] { (a, b) =>
    if (a eq b) 0 else a.compareTo(b)
  }


  given KeyEncoder[Player] = { player => player }

  given KeyDecoder[Player] = _.some


  given CirceCodec[Player] =
    CirceCodec.from(Decoder[String], Encoder[String]).imap(Player.apply)(identity)


  given Schema[Player] = Schema.string[Player]


  given TapirCodec[String, Player, TextPlain] =
    TapirCodec.string.map(Player.apply)(identity)
