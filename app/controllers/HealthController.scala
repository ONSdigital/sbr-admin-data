package controllers

import io.swagger.annotations.{ Api, ApiOperation, ApiResponse, ApiResponses }
import org.joda.time.DateTime
import play.api.mvc.{ Action, Controller }

@Api("Health")
class HealthController extends Controller {
  private[this] val startTime = System.currentTimeMillis()

  @ApiOperation(
    value = "Health endpoint",
    notes = "This endpoints shows the API uptime",
    httpMethod = "GET"
  )
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Success - Displays API health")
  ))
  def health = Action {
    val uptimeInMillis = uptime()
    Ok(s"{Status: Ok, Uptime: ${uptimeInMillis}ms, Date and Time: " + new DateTime(startTime) + "}").as(JSON)
  }

  private def uptime(): Long = {
    val uptimeInMillis = System.currentTimeMillis() - startTime
    uptimeInMillis
  }
}