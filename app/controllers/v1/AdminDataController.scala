package controllers.v1

/**
 * Created by coolit on 16/11/2017.
 */
import javax.inject.Inject

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

import akka.pattern.ask
import play.api.cache.CacheApi
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Result}
import org.joda.time.YearMonth
import org.joda.time.format.DateTimeFormat
import com.typesafe.scalalogging.LazyLogging

import config.Properties._
import models.ValidLookup
import hbase.model.AdminData
import hbase.repository.AdminDataRepository
import utils.LookupValidator.REFERENCE_PERIOD_FORMAT
import utils.{LookupValidator, Utilities}

/**
 * Created by coolit on 07/11/2017.
 */
class AdminDataController @Inject() (repository: AdminDataRepository, cache: CacheApi, val messagesApi: MessagesApi) extends ControllerUtils with I18nSupport with LazyLogging {

  val cb = getCircuitBreaker(getRecordById)

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
    // Do the db call through a circuit breaker
    val askFuture = breaker.withCircuitBreaker(cb ? v).mapTo[Future[Option[AdminData]]]
    askFuture.flatMap(x => x.map(
      y => y match {
        case Some(s) => {
          cache.set(cacheKey, s, cacheDuration)
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

  def getRecordById(v: ValidLookup): Future[Option[AdminData]] = repository.lookup(v.period, v.id)

  def lookupRest(period: String, id: String): Action[AnyContent] = {
    Action.async {
      Try(YearMonth.parse(period, DateTimeFormat.forPattern(REFERENCE_PERIOD_FORMAT))) match {
        case Success(date: YearMonth) =>
          repository.lookupRest(id, date).map {
            case response if response.status == OK => {
              Ok(response.body).as(JSON)
            }
            //TODO - add not found message
            case response if response.status == NOT_FOUND => NotFound(response.body).as(JSON)
          } recover responseException
        case Failure(ex: IllegalArgumentException) =>
          BadRequest(errAsJson(BAD_REQUEST, "bad_request", s"Invalid date argument $period - must conform to YearMonth [yyyyMM]")).future
        case Failure(ex) => BadRequest(errAsJson(BAD_REQUEST, "bad_request", s"$ex")).future
      }
    }
  }
}
