package com.github.esgott.spottle.edge


import cats.effect.kernel.Sync

import scala.util.Random


trait GameIdGenerator[F[_]]:
  def nextGameid: F[Long]


object GameIdGenerator:

  def gameIdGenerator[F[_]: Sync](seed: Long): GameIdGenerator[F] = new GameIdGenerator[F]:
    private val random               = Random(seed)
    override def nextGameid: F[Long] = randomLong(random)


  def randomLong[F[_]: Sync](random: Random = Random()): F[Long] =
    Sync[F].delay(random.nextLong())
