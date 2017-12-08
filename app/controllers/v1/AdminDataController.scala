package controllers.v1

/**
 * Created by coolit on 16/11/2017.
 */
import javax.inject.Inject

import scala.concurrent.Future

import akka.pattern.ask
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.cache.CacheApi
import play.api.i18n.{ I18nSupport, Messages, MessagesApi }
import play.api.mvc.{ Action, AnyContent }
import org.joda.time.format.DateTimeFormat
import com.github.nscala_time.time.Imports._
import com.typesafe.scalalogging.LazyLogging
import io.swagger.annotations._

import models.ValidLookup
import utils.{ LookupValidator, Utilities }
import hbase.repository.AdminDataRepository
import config.Properties._
import model.AdminData

@Api("Lookup")
class AdminDataController @Inject() (repository: AdminDataRepository, val messagesApi: MessagesApi, cache: CacheApi) extends ControllerUtils with I18nSupport with LazyLogging {

  val validator = new LookupValidator(messagesApi)
  val cb = getCircuitBreaker(getRecordById)

  // Use hard coded default period until Option[Period] works
  val defaultPeriod = YearMonth.parse("201706", DateTimeFormat.forPattern(AdminData.REFERENCE_PERIOD_FORMAT))

  @ApiOperation(
    value = "Endpoint for getting a record by id and optional period",
    notes = "Period is optional, a default period is used if none is provided",
    responseContainer = "JSONObject",
    code = 200,
    httpMethod = "GET"
  )
  @ApiResponses(Array(
    new ApiResponse(code = 200, responseContainer = "JSONObject", message = "Success -> Record found for id."),
    new ApiResponse(code = 400, responseContainer = "JSONObject", message = "Bad Request -> Invalid parameters."),
    new ApiResponse(code = 404, responseContainer = "JSONObject", message = "Not Found -> Id not found."),
    new ApiResponse(code = 500, responseContainer = "JSONObject", message = "Internal Server Error -> Request could not be completed.")
  ))
  def lookup(
    @ApiParam(value = "A valid period in yyyyMM format", example = "201706", required = false) period: Option[String],
    @ApiParam(value = "An id, validated using the validation.id environment variable regex", example = "123456", required = true) id: String
  ): Action[AnyContent] = Action.async { implicit request =>
    logger.info(s"Lookup with period [$period] for id [$id]")
    validator.validateLookupParams(id, period) match {
      case Right(v) => cache.getOrElse[Future[Result]](Utilities.createCacheKey(v), cacheDuration)(repositoryLookup(v))
      case Left(error) => BadRequest(Utilities.errAsJson(BAD_REQUEST, "Bad Request", error.msg)).future
    }
  }

  def repositoryLookup(v: ValidLookup): Future[Result] = {
    // Do the db call through a circuit breaker
    val askFuture = breaker.withCircuitBreaker(cb ? v).mapTo[Future[Option[AdminData]]]
    askFuture.flatMap(x => x.map(y => y match {
      case Some(s) => Ok(Json.toJson(s))
      case None => NotFound(Utilities.errAsJson(NOT_FOUND, "Not Found", Messages("controller.not.found", v.id)))
    })).recover({
      case _ => {
        logger.error(s"Unable to get record from database")
        InternalServerError(Utilities.errAsJson(INTERNAL_SERVER_ERROR, "Internal Server Error", Messages("controller.server.error")))
      }
    })
  }

  def getRecordById(v: ValidLookup): Future[Option[AdminData]] = repository.lookup(v.period.getOrElse(defaultPeriod), v.id)
}