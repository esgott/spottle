package com.github.esgott.spottle.api


import cats.Order

import io.circe.{Decoder, Encoder, Json, KeyEncoder}


// TODO migrate to circe-generic-extras when Scala 3 is supported by it

type Card = Set[Symbol]

opaque type Symbol = String


object Symbol:
  def apply(s: String): Symbol = s

  given Encoder[Symbol] = Json.fromString(_)

  given Decoder[Symbol] = _.as[String].map(Symbol.apply)


opaque type Player = String


object Player:

  def apply(s: String): Player = s


  given Order[Player] = Order.from[Player] { (a, b) =>
    if (a eq b) 0 else a.compareTo(b)
  }


  given KeyEncoder[Player] = { player => player }

  given Encoder[Player] = Json.fromString(_)

  given Decoder[Player] = _.as[String].map(Player.apply)
