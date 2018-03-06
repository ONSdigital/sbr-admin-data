package hbase.model

import com.github.nscala_time.time.Imports.YearMonth
import play.api.libs.json._
import play.api.libs.functional.syntax._

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

  //  implicit val yearMonthReads: Reads[YearMonth] = (JsPath \ "referencePeriod").read[YearMonth]
  //  implicit val adminDataReads: Reads[AdminData] = (
  //    (JsPath \ "referencePeriod").read[String] and
  //    (JsPath \ "id").read[String] and
  //    (JsPath \ "variables").read[Map[String, String]])(AdminData.apply _)

  //  implicit val readsAdminData: Reads[AdminData] = new Reads[AdminData] {
  //    def reads(json: JsValue): JsResult[AdminData] = {
  //      for {
  //        referencePeriod <- (json \ "referencePeriod").validate[YearMonth]
  //        id <- (json \ "id").validate[String]
  //        variables <- (json \ "variables").validate[Map[String, String]]
  //      } yield AdminData(json, id)
  //    }
  //  }

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