package utils

import java.util.Optional

import config.Properties._
import models.ValidLookup
import play.api.libs.json.{ JsObject, Json }

/**
 * Created by coolit on 16/11/2017.
 */
object Utilities {
  /**
   * Pass parameters to form a JSON response for a request
   */
  def errAsJson(status: Int, code: String, msg: String): JsObject = {
    Json.obj(
      "status" -> status,
      "code" -> code,
      "message_en" -> msg
    )
  }

  /**
   * Convert a Java Optional to a Scala Option
   */
  protected def toOption[X](o: Optional[X]) = if (o.isPresent) Some(o.get) else None

  def createCacheKey(v: ValidLookup): String = List(v.id, v.period.getOrElse(None)).mkString(cacheDelimiter)
}