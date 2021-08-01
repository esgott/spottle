package com.github.esgott.spottle.api

import io.circe.Json


object circe:

  def typeDescriptor(name: String) =
    Json.obj("type" -> Json.fromString(name))
