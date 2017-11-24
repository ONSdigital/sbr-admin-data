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

import scala.util.{ Failure, Success, Try }

/**
 * Created by coolit on 07/11/2017.
 */
class AdminDataController @Inject() (repository: AdminDataRepository, cache: CacheApi) extends ControllerUtils with LazyLogging {

  // val cb = getCircuitBreaker(repositoryLookup)

  def lookup(period: Option[String], id: String): Action[AnyContent] = Action.async { implicit request =>
    logger.info(s"Lookup with period [$period] for id [$id]")
    LookupValidator.validateLookupParams(id, period) match {
      case Right(v) => {
        val cacheKey = List(v.id, v.period.getOrElse(None)).mkString(cacheDelimiter)
        cache.get[Future[Result]](cacheKey).getOrElse(repositoryLookup(v, cacheKey))
      }
      case Left(error) => BadRequest(Utilities.errAsJson(BAD_REQUEST, "Bad Request", error.msg)).future
    }
  }

  def repositoryLookup(v: ValidLookup, cacheKey: String): Future[Result] = {
    Try(repository.lookup(v.period.get, v.id).map(x => x match {
      case Some(s) => {
        val resp = Ok(Json.toJson(s)).future
        cache.set(cacheKey, resp, cacheDuration)
        resp
      }
      case None => NotFound(Utilities.errAsJson(NOT_FOUND, "Not Found", Messages("controller.not.found", v.id))).future
    }).flatMap(x => x)) match {
      case Success(s) => s
      case Failure(ex) => InternalServerError(Utilities.errAsJson(INTERNAL_SERVER_ERROR, "Internal Server Error", Messages("controller.server.error", v.id))).future
    }
  }
}
