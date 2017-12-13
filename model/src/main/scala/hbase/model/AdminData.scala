package hbase.model

import com.github.nscala_time.time.Imports.YearMonth
import play.api.libs.json.{ JsValue, Json, Writes }

/**
 * AdminData
 * ----------------
 * Author: haqa
 * Date: 23 October 2017 - 20:41
 * Copyright (c) 2017  Office for National Statistics
 */

case class AdminData(
  referencePeriod: YearMonth,
  id: String,
  variables: Map[String, String] = Map()) {
  def putVariable(newMap: Map[String, String]): AdminData = this.copy(variables = this.variables ++
    newMap)
}

object AdminData {

  val REFERENCE_PERIOD_FORMAT = "yyyyMM"

  implicit val writer: Writes[AdminData] = new Writes[AdminData] {
    def writes(a: AdminData): JsValue = {
      Json.obj(
        "period" -> a.referencePeriod.toString(REFERENCE_PERIOD_FORMAT),
        "id" -> a.id,
        "variables" -> a.variables)
    }
  }

}
