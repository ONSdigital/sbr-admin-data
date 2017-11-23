package controllers.v1

/**
 * Created by coolit on 16/11/2017.
 */
import javax.inject.Inject

import akka.actor.{ ActorSystem, Props }
import akka.pattern.{ CircuitBreaker, ask }
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import model.AdminData
import models.{ LookupDefaultPeriod, LookupSpecificPeriod, ValidLookup }
import play.api.Configuration
import play.api.Play.current
import play.api.cache.CacheApi
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.libs.json.{ Writes, _ }
import play.api.mvc.{ Action, AnyContent, Controller, Result }
import repository.AdminDataRepository
import utils.{ CircuitBreakerActor, ControllerUtils, LookupValidator }

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scalaz.{ -\/, \/- }
import com.github.nscala_time.time.Imports.YearMonth

import scala.util.{ Failure, Success, Try }

/**
 * Created by coolit on 07/11/2017.
 */
class AdminDataControllerScala @Inject() (repository: AdminDataRepository, cache: CacheApi, config: Configuration) extends Controller with ControllerUtils with LazyLogging {

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

    // http://appliedscala.com/blog/2016/scalaz-disjunctions/
    // http://eed3si9n.com/learning-scalaz/Either.html
    LookupValidator.validateLookupParams(period, id) match {
      case \/-(validParams) => checkCache(validParams)
      case -\/(error) => BadRequest(errAsJson(BAD_REQUEST, "Bad Request", error.msg)).future
    }
  }

  def checkCache(v: ValidLookup): Future[Result] = {
    // Before we do a search, we need to check the cache
    val cacheKey = createCacheKey(v)
    cache.get[AdminData](cacheKey) match {
      case Some(data) => {
        logger.info(s"Returning cached data for cache key: $cacheKey")
        Ok(Json.toJson(data)).future
      }
      case None => getAdminData(v, cacheKey)
    }
  }

  def getAdminData(v: ValidLookup, cacheKey: String): Future[Result] = {
    // Firstly, test the method without the cb:
    repositoryLookup(v).map(x => x match {
      case Some(s) => Ok(Json.toJson(s)).future
      case None => NotFound(errAsJson(NOT_FOUND, "Not Found", Messages("controller.not.found", v.id))).future
    }).flatMap(x => x)
  }

  def repositoryLookup(v: ValidLookup): Future[Option[AdminData]] = v match {
    case a: LookupSpecificPeriod => repository.lookup(a.period, a.id)
    case b: LookupDefaultPeriod => repository.lookup(getDefaultPeriod(), b.id)
  }

  def setCache(cacheKey: String, data: AdminData, duration: Duration): Unit = {
    logger.debug(s"Setting cache for record with id [${data.id}] for $duration")
    cache.set(cacheKey, data, duration)
  }

  def createCacheKey(v: ValidLookup): String = (v match {
    case a: LookupSpecificPeriod => List(a.id, a.period.toString.filter(_ != '-'))
    case b: LookupDefaultPeriod => List(b.id, getDefaultPeriod())
  }).mkString(CACHE_DELIMITER)

  def getDefaultPeriod(): YearMonth = {
    val cacheKey = s"DEFAULT_PERIOD"
    cache.get[YearMonth](cacheKey).getOrElse({
      val period = Await.result(repository.getCurrentPeriod, 1 second)
      cache.set(cacheKey, period, CACHE_DEFAULT_PERIOD_DURATION)
      period
    })
  }
}

