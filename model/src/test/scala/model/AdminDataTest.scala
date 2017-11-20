package model

import com.github.nscala_time.time.Imports.YearMonth
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

/**
 * AdminDataTest
 * ----------------
 * Author: haqa
 * Date: 23 October 2017 - 20:41
 * Copyright (c) 2017  Office for National Statistics
 */

class AdminDataTest extends PlaySpec {

  private val EXPECTED_JSON = """{"period":"2017-06","id":"12345","type":"PAYE","vars":{"Employees":"10"}}"""

  "Create AdminData instance" must {
    "make AdminData and add employee val with 10 in json form" in {
      // joda.time => YearMonth(2017,6)
      val testAdminData: AdminData = new AdminData(YearMonth.parse("201706"), "12345", AdminDataType.PAYE)
      val update = testAdminData.putVariable("Employees", "10")
      val json = Json.toJson(update)
      json.toString mustEqual EXPECTED_JSON
      (json \ "vars" \ "Employees").as[String] mustEqual "10"
    }
  }
}
