package controllers

import org.joda.time.DateTime
import play.api.mvc.{ Action, Controller }

class HealthController extends Controller {
  private[this] val startTime = System.currentTimeMillis()

  def health = Action {
    val uptimeInMillis = uptime()
    Ok(s"{Status: Ok, Uptime: ${uptimeInMillis}ms, Date and Time: " + new DateTime(startTime) + "}").as(JSON)
  }

  private def uptime(): Long = {
    val uptimeInMillis = System.currentTimeMillis() - startTime
    uptimeInMillis
  }

}