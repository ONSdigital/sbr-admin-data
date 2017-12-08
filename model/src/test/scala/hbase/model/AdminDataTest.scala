package hbase.model

import org.scalatest.{ FlatSpec, Matchers }
import com.github.nscala_time.time.Imports.YearMonth
import play.api.libs.json.Json

/**
 * AdminDataTest
 * ----------------
 * Author: haqa
 * Date: 23 October 2017 - 20:41
 * Copyright (c) 2017  Office for National Statistics
 */

class AdminDataTest extends FlatSpec with Matchers {

  private val EXPECTED_JSON = """{"period":"20170601","id":"12345","variables":{"Employees":"10"}}"""

  behavior of "AdminData Object"

  it must "create AdminData instance and add employee val with 10 in json form" in {
    val testAdminData: AdminData = new AdminData(YearMonth.parse("201706"), "12345")
    val update = testAdminData.putVariable(Map("Employees" -> "10"))
    val json = Json.toJson(update)
    json.toString should equal(EXPECTED_JSON)
    (json \ "variables" \ "Employees").as[String] should equal("10")
  }
}
