package com.github.esgott.spottle.api

import cats.Order

type Card = Set[Symbol]

opaque type Symbol = String


object Symbol:
  def apply(s: String): Symbol = s


opaque type Player = String


object Player:

  def apply(s: String): Player = s


  given Order[Player] = Order.from[Player] { (a, b) =>
    if (a eq b) 0 else a.compareTo(b)
  }
