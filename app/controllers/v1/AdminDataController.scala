package controllers.v1

import javax.inject.Inject

import com.typesafe.scalalogging.LazyLogging
import config.Properties
import hbase.model.AdminData.REFERENCE_PERIOD_FORMAT
import hbase.repository.AdminDataRepository
import io.swagger.annotations._
import org.joda.time.YearMonth
import org.joda.time.format.DateTimeFormat
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.i18n.{ I18nSupport, Messages, MessagesApi }
import play.api.mvc.{ Action, AnyContent }
import utils.Utilities

import scala.util.{ Failure, Success, Try }

@Api("Lookup")
class AdminDataController @Inject() (repository: AdminDataRepository, val messagesApi: MessagesApi, cache: CacheApi, val configuration: Configuration)
  extends ControllerUtils with I18nSupport with LazyLogging with Utilities with Properties {

  //  val validator = new LookupValidator(messagesApi, configuration)
  //  val cb = getCircuitBreaker(getRecordById)
  //
  //  @ApiOperation(
  //    value = "Endpoint for getting a record by id and optional period",
  //    notes = "Period is optional, a default period is used if none is provided",
  //    responseContainer = "JSONObject",
  //    httpMethod = "GET")
  //  @ApiResponses(Array(
  //    new ApiResponse(code = 200, responseContainer = "JSONObject", message = "Success -> Record found for id."),
  //    new ApiResponse(code = 400, responseContainer = "JSONObject", message = "Bad Request -> Invalid parameters."),
  //    new ApiResponse(code = 404, responseContainer = "JSONObject", message = "Not Found -> Id not found."),
  //    new ApiResponse(code = 500, responseContainer = "JSONObject", message = "Internal Server Error -> Request could not be completed.")))
  //  def lookup(
  //    @ApiParam(value = "A valid period in yyyyMM format", example = "201706", required = false) period: Option[String],
  //    @ApiParam(value = "An id, validated using the validation.id environment variable regex", example = "123456", required = true) id: String): Action[AnyContent] = Action.async { implicit request =>
  //    logger.info(s"Lookup with period [$period] for id [$id]")
  //    validator.validateLookupParams(id, period) match {
  //      case Right(v) => cache.getOrElse[Future[Result]](createCacheKey(v), cacheDuration)(repositoryLookup(v))
  //      case Left(error) => BadRequest(errAsJson(BAD_REQUEST, "Bad Request", error.msg)).future
  //    }
  //  }
  //
  //  def repositoryLookup(v: ValidLookup): Future[Result] = {
  //    // Do the db call through a circuit breaker
  //    val askFuture = breaker.withCircuitBreaker(cb ? v).mapTo[Future[Option[AdminData]]]
  //    askFuture.flatMap(x => x.map(
  //      y => y match {
  //        case Some(s) => {
  //          Ok(Json.toJson(s))
  //        }
  //        case None => NotFound(errAsJson(NOT_FOUND, "Not Found", Messages("controller.not.found", v.id)))
  //      })).recover({
  //      case _ => {
  //        logger.error(s"Unable to get record from database")
  //        InternalServerError(errAsJson(INTERNAL_SERVER_ERROR, "Internal Server Error",
  //          Messages("controller.server.error")))
  //      }
  //    })
  //  }
  //
  //  def getRecordById(v: ValidLookup): Future[Option[AdminData]] = repository.lookup(v.period, v.id)

  def lookup(period: Option[String], max: Option[Long], id: String): Action[AnyContent] = {
    val maxRespSize = max.getOrElse(AdminDataRepository.MAX_RESULT_SIZE)
    Action.async {
      period match {
        case Some(p) =>
          Try(YearMonth.parse(p, DateTimeFormat.forPattern(REFERENCE_PERIOD_FORMAT))) match {
            case Success(date: YearMonth) =>
              lookupRequest(repository.lookup, Some(date), id, maxRespSize)
            case Failure(ex: IllegalArgumentException) =>
              BadRequest(Messages("controller.invalid.period", p, REFERENCE_PERIOD_FORMAT, ex.toString)).future
            case Failure(ex) => BadRequest(s"$ex").future
          }
        case None =>
          lookupRequest(repository.lookup, None, id, maxRespSize)
      }
    }
  }
}
