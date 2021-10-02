package com.github.esgott.spottle.api.http.v1

import io.circe.Codec

sealed trait SpottleError


object SpottleError:

  case class NotFound(message: String) extends SpottleError derives Codec.AsObject

  case class BadRequest(message: String) extends SpottleError derives Codec.AsObject
