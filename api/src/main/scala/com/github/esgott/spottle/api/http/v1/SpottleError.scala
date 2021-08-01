package com.github.esgott.spottle.api.http.v1


import cats.syntax.either._
import com.github.esgott.spottle.api.circe.typeDescriptor
import io.circe.{Decoder, DecodingFailure, Encoder, Json}
import io.circe.CursorOp.DownField
import io.circe.generic.auto._
import io.circe.syntax._


// TODO migrate to circe-generic-extras when https://github.com/circe/circe-generic-extras/issues/168 is solved

enum SpottleError:
  case NotFound


object SpottleError:

  given Encoder[SpottleError] = { case NotFound =>
    typeDescriptor("NotFound")
  }


  given Decoder[SpottleError] = { cursor =>
    for
      name <- cursor.downField("type").as[String]
      result <- name match
        case "NotFound" => NotFound.asRight
    yield result
  }
