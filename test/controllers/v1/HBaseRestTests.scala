package controllers.v1

import resource.TestUtils
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import hbase.model.AdminData
import play.api.libs.json.{ JsArray, JsSuccess }
import play.api.test.Helpers.{ contentAsJson, contentType, status }
import akka.util.Timeout

import spray.json.JsValue

import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Created by coolit on 13/02/2018.
 */

//    Andy - In the mockEndPoint there's a match to a Some(p) and to a None. When running sbt "testOnly *HBaseRestTests" with the None in the mockEndPoint parameters
//    it works fine. However, when the Some(Period) is used it seems to be ignoring the body of the block that it's in and taking it from the one directly above, if
//    it has the same company number the test passes, but it produces an error otherwise.

class HBaseRestTests extends TestUtils with BeforeAndAfterEach with GuiceOneAppPerSuite {

  private val version = "v1"
  private val nameSpace = "sbr_local_db"
  private val adminDataTable = "enterprise"
  private val columnFamily = "d"
  private val firstPeriod = "201706"
  private val secondPeriod = "201708"

  // We don't use the normal HBase REST port as it can make testing annoying, this is set as a Java Option
  // in the build.sbt
  val port = 8081
  val host = "localhost"
  val wireMockServer = new WireMockServer(wireMockConfig().port(port))

  override def beforeEach {
    wireMockServer.start()
    WireMock.configureFor(host, port)
  }

  override def afterEach {
    wireMockServer.stop()
  }

  //   TODO:
  //   - test each type of endpoint
  //   - test with auth details (this requires small modifications to the configuration)
  //   - we could read in the test data and base64 encode it etc. rather than just returning the data string

  def mockEndpoint(tableName: String, id: String, period: Option[String], body: String): Unit = {
    val path = period match {
      case Some(p) => s"/$nameSpace:$tableName/$id~$p"
      case None => s"/$nameSpace:$tableName/$id~*"
    }
    stubFor(get(urlEqualTo(path))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withHeader("content-type", "application/json")
          .withHeader("transfer-encoding", "chunked")
          .withBody(body)))
  }

  "/v1/records/:id" should {
    "return a unit for a valid id (ch)" in {
      implicit val duration: Timeout = 100 seconds

      val id = "03007252"
      val body = "{\"Row\":[{\"key\":\"MDMwMDcyNTJ+MjAxNzA2\",\"Cell\":[{\"column\":\"ZDppZA==\",\"timestamp\":1519736664006,\"$\":\"MDMwMDcyNTI=\"},{\"column\":\"ZDpuYW1l\",\"timestamp\":1519736831889,\"$\":\"YmlnIGNvbXBhbnkgMTIz\"},{\"column\":\"ZDpwZXJpb2Q=\",\"timestamp\":1519736810004,\"$\":\"MjAxNzA2\"}]}]}"
      mockEndpoint(adminDataTable, id, None, body)
      val resp = fakeRequest(s"/$version/records/$id")
      val json = contentAsJson(resp).as[JsArray]
      contentType(resp) mustBe Some("application/json")
      json.value.size mustBe 1
      val js = json.as[JsArray]

      val idUnit = (js.head \ "id").as[String]
      idUnit mustBe id
      val periodUnit = (js.head \ "period").as[String]
      periodUnit mustBe firstPeriod
      val nameUnit = ((js.head \ "variables") \ "name").as[String]
      nameUnit mustBe "big company 123"
    }
  }
  "/v1/records/:id/periods/:period" should {
    "return a unit for a valid id (ch)" in {
      implicit val duration: Timeout = 100 seconds

      val id = "03007252"
      val body = "{\"Row\":[{\"key\":\"MDMwMDcyNTJ+MjAxNzA2\",\"Cell\":[{\"column\":\"ZDppZA==\",\"timestamp\":1519736664006,\"$\":\"MDMwMDcyNTI=\"},{\"column\":\"ZDpuYW1l\",\"timestamp\":1519736831889,\"$\":\"YmlnIGNvbXBhbnkgMTIz\"},{\"column\":\"ZDpwZXJpb2Q=\",\"timestamp\":1519736810004,\"$\":\"MjAxNzA2\"}]}]}"
      val period = "201706"
      mockEndpoint(adminDataTable, id, Some(period), body)
      val resp = fakeRequest(s"/$version/records/$id")
      val json = contentAsJson(resp).as[JsArray]
      contentType(resp) mustBe Some("application/json")
      json.value.size mustBe 1
      val js = json.as[JsArray]

      val idUnit = (js.head \ "id").as[String]
      idUnit mustBe id
      val periodUnit = (js.head \ "period").as[String]
      periodUnit mustBe firstPeriod
      val nameUnit = ((js.head \ "variables") \ "name").as[String]
      nameUnit mustBe "big company 123"
    }
  }

  "/v1/records/:id/history" should {
    "return a unit for a valid id (ch)" in {
      implicit val duration: Timeout = 100 seconds

      val id = "00032311"
      val body = "{\"Row\":[{\"key\":\"MDAwMzIzMTF+MjAxNzA2\",\"Cell\":[{\"column\":\"ZDppZA==\",\"timestamp\":1519736664006,\"$\":\"MDAwMzIzMTE==\"},{\"column\":\"ZDpuYW1l\",\"timestamp\":1519736831889,\"$\":\"YmlnIGNvbXBhbnkgMTIz\"},{\"column\":\"ZDpwZXJpb2Q=\",\"timestamp\":1519736810004,\"$\":\"MjAxNzA2\"}]}]}"
      val period = "201706"
      mockEndpoint(adminDataTable, id, None, body)
      val resp = fakeRequest(s"/$version/records/$id")
      val json = contentAsJson(resp).as[JsArray]
      contentType(resp) mustBe Some("application/json")
      json.value.size mustBe 1
      val js = json.as[JsArray]

      val idUnit = (js.head \ "id").as[String]
      idUnit mustBe id
      val periodUnit = (js.head \ "period").as[String]
      periodUnit mustBe firstPeriod
      val nameUnit = ((js.head \ "variables") \ "name").as[String]
      nameUnit mustBe "big company 123"
    }
  }

  //
  //  "/v1/records/:id" should {
  //    "return a unit for a valid id (ch)" in {
  //      implicit val duration: Timeout = 100 seconds
  //      val id = "201706"
  //      val body = "{\"Row\":[{\"key\":\"MDMwMDcyNTJ+MjAxNzA2\",\"Cell\":[{\"column\":\"ZDppZA==\",\"timestamp\":1519736664006,\"$\":\"MDMwMDcyNTI=\"},{\"column\":\"ZDpuYW1l\",\"timestamp\":1519736831889,\"$\":\"YmlnIGNvbXBhbnkgMTIz\"},{\"column\":\"ZDpwZXJpb2Q=\",\"timestamp\":1519736810004,\"$\":\"MjAxNzA2\"}]}]}"
  //      mockEndpoint(adminDataTable, firstPeriod, id, body)
  //      val resp = fakeRequest(s"/$version/records/$id")
  //      val json = contentAsJson(resp).as[JsArray]
  //              val unit = json(0).validate[AdminData]
  //              //status(resp) mustBe OK
  //              contentType(resp) mustBe Some("application/json")
  //              json.value.size mustBe 1
  //              unit.isInstanceOf[JsSuccess[AdminData]] mustBe true
  //      (1 must equal(1))
  //    }
  //  }
}
//put 'sbr_local_db:ch' , '03007252~201706', 'd:period', '201706'
//put 'sbr_local_db:ch' , '03007252~201706', 'd:name', 'big company 123'
