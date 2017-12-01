package controllers

import io.swagger.annotations.{ Api, ApiOperation, ApiResponse, ApiResponses }
import org.joda.time.DateTime
import play.api.libs.json.Json
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
    val dateTime = new DateTime(startTime)
    Ok(Json.obj(
      "startTime" -> s"${dateTime.toLocalDate} ${dateTime.toLocalTime}",
      "uptime" -> millisToHoursMinutesDays(uptimeInMillis)
    ))
  }

  private def millisToHoursMinutesDays(millis: Long): String = {
    val days = (millis / (1000 * 60 * 60 * 24))
    val hours = (millis / (1000 * 60 * 60) % 24)
    val minutes = (millis / (1000 * 60) % 60)
    s"$days days $hours hours $minutes minutes"
  }

  private def uptime(): Long = {
    val uptimeInMillis = System.currentTimeMillis() - startTime
    uptimeInMillis
  }
}