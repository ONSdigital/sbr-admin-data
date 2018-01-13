package controllers.v1

import java.time.format.DateTimeParseException
import javax.naming.ServiceUnavailableException

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.CircuitBreaker
import akka.util.Timeout
import com.github.nscala_time.time.Imports.YearMonth
import com.typesafe.scalalogging.LazyLogging
import config.Properties
import hbase.model.AdminData
import play.api.Configuration
import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Controller, Result}
import utils.CircuitBreakerActor

import scala.concurrent.duration._
import scala.concurrent.{Future, TimeoutException}

/**
 * AdminDataControllerUtils
 * ----------------
 * Author: haqa
 * Date: 22 November 2017 - 14:19
 * Copyright (c) 2017  Office for National Statistics
 */
trait ControllerUtils extends Controller with LazyLogging with Properties with I18nSupport {

  val system = ActorSystem("sbr-admin-data-circuit-breaker")
  implicit val ec = system.dispatcher

  val breaker =
    new CircuitBreaker(
      system.scheduler,
      maxFailures = cbMaxFailures,
      callTimeout = cbCallTimeout,
      resetTimeout = cbResetTimeout).
      onOpen(logger.warn("----- circuit breaker opened! -----")).
      onClose(logger.warn("----- circuit breaker closed! -----")).
      onHalfOpen(logger.warn("----- circuit breaker half-open -----"))

  implicit val configuration: Configuration
  implicit val timeout = Timeout(configuration.getMilliseconds("akka.ask.timeout").map(_ millis).getOrElse(2 seconds))

  def getCircuitBreaker[T, Z](f: T => Future[Option[Z]]): ActorRef = system.actorOf(Props(new CircuitBreakerActor(f)))

  /**
   * On a result, use .future, e.g. Ok().future
   * Method source: https://github.com/outworkers/hbase.util/blob/develop/hbase.util-play/src/main/scala/com/outworkers/hbase.util/play/package.scala#L98
   */
  implicit class ResultAugmenter(val res: Result) {
    def future: Future[Result] = Future.successful(res)
  }

  def responseException: PartialFunction[Throwable, Result] = {
    case ex: DateTimeParseException =>
      logger.error("cannot parse date to to specified date format", ex)
      BadRequest(errAsJson(BAD_REQUEST, "invalid_date", s"cannot parse date exception found $ex"))
    case ex: RuntimeException =>
      logger.error(s"RuntimeException $ex", ex.getCause)
      InternalServerError(errAsJson(INTERNAL_SERVER_ERROR, "runtime_exception", ex.getMessage))
    case ex: ServiceUnavailableException =>
      logger.error(s"ServiceUnavailableException $ex", ex.getCause)
      ServiceUnavailable(errAsJson(SERVICE_UNAVAILABLE, "service_unavailable", s"$ex"))
    case ex: TimeoutException =>
      logger.error(s"TimeoutException $ex", ex.getCause)
      RequestTimeout(errAsJson(REQUEST_TIMEOUT, "request_timeout", s"This may be due to connection being blocked. $ex"))
    case ex =>
      logger.error(s"Unknown error has occurred with exception $ex", ex.getCause)
      InternalServerError(errAsJson(INTERNAL_SERVER_ERROR, "internal_server_error", s"$ex."))
  }

  protected def errAsJson(status: Int, code: String, msg: String, cause: String = "Not traced"): JsObject = {
    Json.obj(
      "status" -> status,
      "code" -> code,
      "route_with_cause" -> cause,
      "message_en" -> msg)
  }

  protected def lookupRequest(
                               lookup: (Option[YearMonth], String, Long) => Future[Option[Seq[AdminData]]],
                               period: Option[YearMonth], id: String, max: Long): Future[Result] = {
    lookup(period, id, max).map {
      case Some(res: Seq[AdminData]) => Ok(Json.toJson(res))
      case None => if (period.isEmpty) {
        NotFound(Messages("controller.not.found", id))
      } else {
        NotFound(Messages("controller.not.found.with.period", id, period.getOrElse("").toString))
      }
    } recover responseException
  }

}
