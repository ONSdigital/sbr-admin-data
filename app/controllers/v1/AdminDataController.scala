package controllers.v1

import javax.inject.Inject

import scala.util.{ Failure, Success, Try }

import play.api.cache.CacheApi
import play.api.i18n.{ Messages, MessagesApi }
import play.api.mvc.{ Action, AnyContent }
import org.joda.time.YearMonth
import org.joda.time.format.DateTimeFormat
import com.typesafe.scalalogging.LazyLogging

import hbase.repository.AdminDataRepository
import utils.LookupValidator.REFERENCE_PERIOD_FORMAT

class AdminDataController @Inject() (repository: AdminDataRepository, cache: CacheApi, val messagesApi: MessagesApi)
  extends ControllerUtils with LazyLogging {

  //    val cb = getCircuitBreaker(getRecordById)
  //
  //    def lookup(period: Option[String], id: String): Action[AnyContent] = Action.async { implicit request =>
  //      logger.info(s"Lookup with period [$period] for id [$id]")
  //      LookupValidator.validateLookupParams(id, period) match {
  //        case Right(v) => {
  //          val cacheKey = List(v.id, v.period.getOrElse(None)).mkString(cacheDelimiter)
  //          cache.get[Future[Result]](cacheKey).getOrElse(repositoryLookup(v, cacheKey))
  //        }
  //        case Left(error) => BadRequest(Utilities.errAsJson(BAD_REQUEST, "Bad Request", error.msg)).future
  //      }
  //    }
  //
  //    def repositoryLookup(v: ValidLookup, cacheKey: String): Future[Result] = {
  //      // Do the db call through a circuit breaker
  //      val askFuture = breaker.withCircuitBreaker(cb ? v).mapTo[Future[Option[AdminData]]]
  //      askFuture.flatMap(x => x.map(
  //        y => y match {
  //          case Some(s) => {
  //            cache.set(cacheKey, s, cacheDuration)
  //            Ok(Json.toJson(s))
  //          }
  //          case None => NotFound(Utilities.errAsJson(NOT_FOUND, "Not Found", Messages("controller.not.found", v.id)))
  //        })).recover({
  //        case _ => {
  //          logger.error(s"Unable to get record from database")
  //          InternalServerError(Utilities.errAsJson(INTERNAL_SERVER_ERROR, "Internal Server Error",
  //            Messages("controller.server.error")))
  //        }
  //      })
  //    }
  //
  //    def getRecordById(v: ValidLookup): Future[Option[AdminData]] = repository.lookup(v.period, v.id)

  def lookup(period: Option[String], id: String, max: Long = AdminDataRepository.MAX_RESULT_SIZE): Action[AnyContent] = {
    Action.async {
      period match {
        case Some(p) =>
          Try(YearMonth.parse(p, DateTimeFormat.forPattern(REFERENCE_PERIOD_FORMAT))) match {
            case Success(date: YearMonth) =>
              searchResponse(repository.lookup, Some(date), id, max)
            case Failure(ex: IllegalArgumentException) =>
              BadRequest(Messages("controller.invalid.period", p, REFERENCE_PERIOD_FORMAT, ex.toString)).future
            case Failure(ex) => BadRequest(s"$ex").future
          }
        case None =>
          searchResponse(repository.lookup, None, id, max)
      }
    }
  }

}
