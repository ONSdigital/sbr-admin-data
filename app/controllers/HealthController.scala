package controllers

import org.joda.time.DateTime
import play.api.libs.json.Json
import play.api.mvc.{ Action, AnyContent, Controller }

class HealthController extends Controller {
  private[this] val startTime = System.currentTimeMillis()

  def health: Action[AnyContent] = Action {
    Ok(Json.obj(s"Status" -> "Ok", "Uptime" -> s"${System.currentTimeMillis() - startTime}ms",
      "Date and Time" -> new DateTime(startTime).toString)).as(JSON)
  }

}