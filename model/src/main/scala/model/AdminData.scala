package model

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
    //    `type`: AdminDataType,
    variables: Map[String, String] = Map()
) {
  def putVariable(variable: String, value: String): AdminData = this.copy(variables = this.variables ++
    Map(variable -> value))
}

object AdminData {

  final val REFERENCE_PERIOD_FORMAT = "yyyyMM"

  implicit val writer: Writes[AdminData] = new Writes[AdminData] {
    def writes(a: AdminData): JsValue = {
      Json.obj(
        "period" -> a.referencePeriod.toString(REFERENCE_PERIOD_FORMAT),
        "id" -> a.id,
        //        "type" -> a.`type`,
        "vars" -> a.variables
      )
    }
  }

}
