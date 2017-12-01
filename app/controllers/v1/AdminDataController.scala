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
import hbase.repository.AdminDataRepository
import play.api.libs.json.Json
import play.api.mvc.Result
import config.Properties._
import io.swagger.annotations._
import model.AdminData

/**
 * Created by coolit on 07/11/2017.
 */
@Api("Lookup")
class AdminDataController @Inject() (repository: AdminDataRepository, val messagesApi: MessagesApi, cache: CacheApi) extends ControllerUtils with I18nSupport with LazyLogging {

  val validator = new LookupValidator(messagesApi)
  val cb = getCircuitBreaker(getRecordById)

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
      case Right(v) => {
        val cacheKey = createCacheKey(v)
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
          val resp = Ok(Json.toJson(s))
          cache.set(cacheKey, resp.future, cacheDuration)
          resp
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

  def createCacheKey(v: ValidLookup): String = List(v.id, v.period.getOrElse(None)).mkString(cacheDelimiter)
}
