package utils

import java.util.Optional

import play.api.libs.json.{ JsObject, Json }
import play.api.mvc.Result

import scala.concurrent.Future

/**
  * Created by coolit on 16/11/2017.
  */
trait ControllerUtils {

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
    * On a result, use .future, e.g. Ok().future
    * Method source: https://github.com/outworkers/util/blob/develop/util-play/src/main/scala/com/outworkers/util/play/package.scala#L98
    */
  implicit class ResultAugmenter(val res: Result) {
    def future: Future[Result] = Future.successful(res)
  }

  /**
    * Convert a Java Optional to a Scala Option
    */
  protected def toOption[X](o: Optional[X]) = if (o.isPresent) Some(o.get) else None
}
