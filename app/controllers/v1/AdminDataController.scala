package controllers.v1

/**
 * Created by coolit on 16/11/2017.
 */
import javax.inject.Inject

import scala.concurrent.Future
import play.api.Play.current
import play.api.cache.CacheApi
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{ Action, AnyContent }
import com.typesafe.scalalogging.LazyLogging
import model.AdminData
import models.ValidLookup
import utils.{ LookupValidator, Utilities }
import repository.AdminDataRepository

import play.api.libs.json.Json
import play.api.mvc.Result
import config.Properties._

/**
 * Created by coolit on 07/11/2017.
 */
class AdminDataController @Inject()(repository: AdminDataRepository, cache: CacheApi) extends ControllerUtils with LazyLogging {

  val cb = getCircuitBreaker(repositoryLookup)

  def lookup(period: Option[String], id: String): Action[AnyContent] = Action.async { implicit request =>
    logger.info(s"Lookup with period [$period] for id [$id]")
    LookupValidator.validateLookupParams(id, period) match {
      case Right(validParams) => checkCache(validParams)
      case Left(error) => BadRequest(Utilities.errAsJson(BAD_REQUEST, "Bad Request", error.msg)).future
    }
  }

  def checkCache(v: ValidLookup): Future[Result] = {
    val cacheKey = List(v.id, v.period.getOrElse(None)).mkString(cacheDelimiter)
    // https://stackoverflow.com/questions/34612363/cache-getorelse-on-futures-in-playframework

    cache.get[AdminData](cacheKey) match {
      case Some(data) => {
        logger.debug(s"Returning cached data for cache key: $cacheKey")
        Ok(Json.toJson(data)).future
      }
      case None => getAdminData(v, cacheKey)
    }
  }

  def setCache(cacheKey: String, data: AdminData): Unit = {
    logger.debug(s"Setting cache for record with id [${data.id}] for $cacheDuration minutes")
    cache.set(cacheKey, data, cacheDuration)
  }

  def getAdminData(v: ValidLookup, cacheKey: String): Future[Result] = {
    repositoryLookup(v).map(x => x match {
      case Some(s) => {
        setCache(cacheKey, s)
        Ok(Json.toJson(s)).future
      }
      case None => NotFound(Utilities.errAsJson(NOT_FOUND, "Not Found", Messages("controller.not.found", v.id))).future
    }).flatMap(x => x)
  }

  // Use the other method below when lookup takes an Option
  def repositoryLookup(v: ValidLookup): Future[Option[AdminData]] = repository.lookup(v.period.get, v.id)

  //  def repositoryLookup(v: ValidLookup): Future[Option[AdminData]] = repository.lookup(v.period.getOrElse(None), v.id)
}
