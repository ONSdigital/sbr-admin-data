package controllers.v1

/**
 * Created by coolit on 16/11/2017.
 */
import javax.inject.Inject

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scalaz.{-\/, \/-}

import akka.actor.{ActorSystem, Props}
import akka.pattern.{CircuitBreaker, ask}
import akka.util.Timeout
import play.api.Configuration
import play.api.Play.current
import play.api.cache.CacheApi
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.libs.json.{Writes, _}
import play.api.mvc.{Action, AnyContent, Result}
import com.github.nscala_time.time.Imports.YearMonth
import com.typesafe.scalalogging.LazyLogging

import model.AdminData
import models.ValidLookup
import utils.{CircuitBreakerActor, Utilities, LookupValidator}
import repository.AdminDataRepository

import scala.util.{ Failure, Success, Try }

import play.api.libs.json.{ JsObject, Json }
import play.api.mvc.Result

import scala.concurrent.Future

/**
 * Created by coolit on 07/11/2017.
 */
class AdminDataControllerScala @Inject() (repository: AdminDataRepository, cache: CacheApi, config: Configuration) extends ControllerUtils with LazyLogging {

  private val CACHE_DELIMITER: String = "~"
  private val CACHE_DURATION: Duration = config.getInt("cache.admin_data.duration").getOrElse(10) minutes
  private val CACHE_DEFAULT_PERIOD_DURATION: Duration = config.getInt("cache.default_period.duration").getOrElse(60) minutes

  val system = ActorSystem("sbr-admin-data-circuit-breaker")
  implicit val ec = system.dispatcher

  val breaker =
    new CircuitBreaker(
      system.scheduler,
      maxFailures = config.getInt("circuit_breaker.maxFailures").getOrElse(5),
      callTimeout = config.getInt("circuit_breaker.callTimeout").getOrElse(1) seconds,
      resetTimeout = config.getInt("circuit_breaker.resetTimeout").getOrElse(10) seconds
    ).
      onOpen(logger.warn("----- circuit breaker opened! -----")).
      onClose(logger.warn("----- circuit breaker closed! -----")).
      onHalfOpen(logger.warn("----- circuit breaker half-open -----"))

  implicit val timeout = Timeout(2 seconds)
  val db = system.actorOf(Props(new CircuitBreakerActor(repositoryLookup)))

  def lookup(period: Option[String], id: String): Action[AnyContent] = Action.async { implicit request =>
    logger.info(s"Lookup with period [$period] for id [$id]")
    LookupValidator.validateLookupParams(id, period) match {
      case \/-(validParams) => checkCache(validParams)
      case -\/(error) => BadRequest(Utilities.errAsJson(BAD_REQUEST, "Bad Request", error.msg)).future
    }
  }

  def checkCache(v: ValidLookup): Future[Result] = {
    val cacheKey = List(v.id, v.period.getOrElse(None)).mkString(CACHE_DELIMITER)
    cache.get[AdminData](cacheKey) match {
      case Some(data) => {
        logger.debug(s"Returning cached data for cache key: $cacheKey")
        Ok(Json.toJson(data)).future
      }
      case None => getAdminData(v)
    }
  }

  def getAdminData(v: ValidLookup): Future[Result] = {
    repositoryLookup(v).map(x => x match {
      case Some(s) => Ok(Json.toJson(s)).future
      case None => NotFound(Utilities.errAsJson(NOT_FOUND, "Not Found", Messages("controller.not.found", v.id))).future
    }).flatMap(x => x)
  }

  def repositoryLookup(v: ValidLookup): Future[Option[AdminData]] = repository.lookup(v.period.getOrElse(None), v.id)
}

