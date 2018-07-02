package controllers.v1

import play.api.libs.json._
import play.api.mvc.AnyContentAsJson
import play.api.test.FakeRequest
import play.api.test.Helpers._
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

/**
 * TestUtils
 * ----------------
 * Author: haqa
 * Date: 02 February 2018 - 10:07
 * Copyright (c) 2017  Office for National Statistics
 */

trait TestUtils extends PlaySpec with GuiceOneAppPerSuite {

  protected def requestObject(url: String, method: String = GET): FakeRequest[AnyContentAsJson] =
    FakeRequest(GET, "/").withJsonBody(Json.parse("""{ "field": "value" }"""))

  protected def getValue(json: Option[String]): String = json match {
    case Some(x: String) => x.toString
    case _ => sys.error("No Value failed. Forcing test failure")
  }

  protected def getJsValue(elem: JsLookupResult): String = elem match {
    case JsDefined(y) => y.toString
    case _ => sys.error("No JsValue found. Forcing test failure")
  }

  protected def instanceName(s: String, regex: String = "."): String = s.substring(s.lastIndexOf(regex) + 1)

}
