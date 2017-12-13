package services.util

import java.time.format.DateTimeParseException
import javax.naming.ServiceUnavailableException

import scala.concurrent.TimeoutException

import com.google.common.base.Charsets
import com.google.common.io.BaseEncoding
import play.api.http.Status
import play.api.libs.json.{ JsObject, Json }

/**
 * ResponseUtil
 * ----------------
 * Author: haqa
 * Date: 06 December 2017 - 08:55
 * Copyright (c) 2017  Office for National Statistics
 */
object ResponseUtil extends Status {

  def responseException: PartialFunction[Throwable, JsObject] = {
    case ex: DateTimeParseException =>
      errAsJson(BAD_REQUEST, "invalid_date", s"cannot parse date exception found $ex")
    case ex: RuntimeException =>
      errAsJson(INTERNAL_SERVER_ERROR, "runtime_exception", s"$ex", s"${ex.getCause}")
    case ex: ServiceUnavailableException =>
      errAsJson(SERVICE_UNAVAILABLE, "service_unavailable", s"$ex", s"${ex.getCause}")
    case ex: TimeoutException =>
      errAsJson(REQUEST_TIMEOUT, "request_timeout",
        s"This may be due to connection being blocked or host failure. Found exception $ex", s"${ex.getCause}")
    case ex => errAsJson(INTERNAL_SERVER_ERROR, "internal_server_error", s"$ex", s"${ex.getCause}")
  }

  def errAsJson(status: Int, code: String, msg: String, cause: String = "Not traced"): JsObject = {
    Json.obj(
      "status" -> status,
      "code" -> code,
      "route_with_cause" -> cause,
      "message_en" -> msg
    )
  }

  def decodeArrayByte(bytes: Array[Byte]) = bytes.map(_.toChar).mkString

  def encodeToArrayByte(str: String) = str.getBytes("UTF-8")

  def encodeBase64(str: Seq[String], deliminator: String = ":") =
    BaseEncoding.base64.encode(str.mkString(deliminator).getBytes(Charsets.UTF_8))

  def decodeBase64(str: String) =
    new String(BaseEncoding.base64().decode(str), "UTF-8")

}
