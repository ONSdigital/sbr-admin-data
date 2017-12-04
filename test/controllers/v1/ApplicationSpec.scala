package controllers.v1

import org.scalatestplus.play._
import play.api.test.Helpers.{ contentAsString, _ }
import play.api.test._

/**
 * Created by coolit on 04/12/2017.
 */
class ApplicationSpec extends PlaySpec with OneAppPerTest {

  "Routes" should {
    "send 404 on a bad request" in {
      route(app, FakeRequest(GET, "/abcdef")).map(status) mustBe Some(NOT_FOUND)
    }
  }

  private[this] def fakeRequest(uri: String, method: String = GET) =
    route(app, FakeRequest(method, uri)).getOrElse(sys.error(s"Can not find route $uri."))

  "HealthController" should {
    "get the health status" in {
      val health = fakeRequest("/health")
      status(health) mustBe OK
      contentType(health) mustBe Some("application/json")
      contentAsString(health) must include("uptime")
      contentAsString(health) must include("startTime")
    }
  }

  "SearchController" should {
    "return 400 when an incorrect period format is used" in {
      val search = fakeRequest("/v1/periods/1706/records/12345")
      status(search) mustBe BAD_REQUEST
      contentType(search) mustBe Some("application/json")
      contentAsString(search) must include("Invalid period")
    }

    "return 400 when an incorrect id is used" in {
      // This search will fail as the default validation on the id is ".{3,8}"
      val search = fakeRequest("/v1/periods/201706/records/0")
      status(search) mustBe BAD_REQUEST
      contentType(search) mustBe Some("application/json")
      contentAsString(search) must include("ID cannot be empty")
    }

    "return 404 when a record cannot be found" in {
      val search = fakeRequest("/v1/periods/201706/records/99999")
      status(search) mustBe NOT_FOUND
      contentAsString(search) must include("Could not find record")
    }

    "return 200 when a record is found for a specified period" in {
      val id = "03007252"
      val period = "201706"
      val search = fakeRequest(s"/v1/periods/$period/records/$id")
      status(search) mustBe OK
      (contentAsJson(search) \ "id").as[String] mustBe id
      (contentAsJson(search) \ "period").as[String] mustBe period
      (contentAsJson(search) \ "variables" \ "companynumber").as[String] mustBe id
    }

    "return 200 when a record is found for the default period" in {
      val search = fakeRequest("/v1/records/03007252")
      status(search) mustBe OK
    }
  }
}
