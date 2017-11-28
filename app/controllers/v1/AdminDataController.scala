package controllers.v1

/**
 * Created by coolit on 16/11/2017.
 */
import javax.inject.Inject

import scala.concurrent.Future
import play.api.cache.CacheApi
import play.api.i18n.{ I18nSupport, Messages, MessagesApi }
import play.api.mvc.{ Action, AnyContent }
import akka.pattern.ask
import com.typesafe.scalalogging.LazyLogging
import models.ValidLookup
import utils.{ LookupValidator, Utilities }
import repository.AdminDataRepository
import play.api.libs.json.Json
import play.api.mvc.Result
import config.Properties._
import model.AdminData

/**
 * Created by coolit on 07/11/2017.
 */
// Cache should be injected
class AdminDataController @Inject() (repository: AdminDataRepository, val messagesApi: MessagesApi) extends ControllerUtils with I18nSupport with LazyLogging {

  val cb = getCircuitBreaker(getRecordById)

  def lookup(period: Option[String], id: String): Action[AnyContent] = Action.async { implicit request =>
    logger.info(s"Lookup with period [$period] for id [$id]")
    LookupValidator.validateLookupParams(id, period) match {
      case Right(v) => {
        val cacheKey = List(v.id, v.period.getOrElse(None)).mkString(cacheDelimiter)
        //cache.get[Future[Result]](cacheKey).getOrElse(repositoryLookup(v, cacheKey))
        repositoryLookup(v, cacheKey)
      }
      case Left(error) => BadRequest(Utilities.errAsJson(BAD_REQUEST, "Bad Request", error.msg)).future
    }
  }

  def repositoryLookup(v: ValidLookup, cacheKey: String): Future[Result] = {
    // Do the db call through a circuit breaker
    val askFuture = breaker.withCircuitBreaker(cb ? v).mapTo[Future[Option[AdminData]]]
    askFuture.flatMap(x => x.map(
      y => y match {
        case Some(s) => {
          //cache.set(cacheKey, s, cacheDuration)
          Ok(Json.toJson(s))
        }
        case None => NotFound(Utilities.errAsJson(NOT_FOUND, "Not Found", Messages("controller.not.found", v.id)))
      }
    )).recover({
      case _ => {
        logger.error(s"Unable to get record from database")
        InternalServerError(Utilities.errAsJson(INTERNAL_SERVER_ERROR, "Internal Server Error", Messages("controller.server.error")))
      }
    })
  }

  def getRecordById(v: ValidLookup): Future[Option[AdminData]] = repository.lookup(v.period.get, v.id)
}
