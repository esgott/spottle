package com.github.esgott.spottle.engine


import cats.effect.IO
import cats.effect.kernel.Ref
import com.github.esgott.spottle.api.Player
import com.github.esgott.spottle.core.Game
import com.github.esgott.spottle.engine.GameStore.{GameEntry, GameMetadata}


trait GameStore[F[_]]:
  def store(id: Long, game: Game, metadata: GameMetadata): F[Unit]
  def drop(id: Long): F[Unit]
  def get(id: Long): F[Option[GameEntry]]


object GameStore:

  case class GameMetadata(creator: Player)
  case class GameEntry(game: Game, metadata: GameMetadata)


  def apply(): IO[GameStore[IO]] =
    for ref <- Ref[IO].of(Map.empty[Long, GameEntry])
    yield new GameStore[IO]:

      override def store(id: Long, game: Game, metadata: GameMetadata): IO[Unit] =
        ref.update(_.updated(id, GameEntry(game, metadata)))


      override def drop(id: Long): IO[Unit] =
        ref.update(_ - id)


      override def get(id: Long): IO[Option[GameEntry]] =
        for map <- ref.get yield map.get(id)
