//TODO switch back

//import play.api.cache.CacheApi
//import play.api.inject.bind
//import play.api.inject.guice.GuiceApplicationBuilder
//import play.api.test.Helpers.{ contentAsString, _ }
//import play.api.test._
//import play.api.{ Application, Configuration }
//import com.typesafe.config.ConfigFactory
//
//class ApplicationSpec extends TestUtils {
//
//  private val appConfig = ConfigFactory.load("application.test.conf")
//
//  override def fakeApplication(): Application =
//    new GuiceApplicationBuilder()
//      .loadConfig(Configuration(appConfig))
//      .overrides(
//        bind[CacheApi].toInstance(new TestCache(false))
//      //        bind[HBaseConnector].toInstance(new HBaseInMemoryConnector(appConfig.getString("hbase.table.name")))
//      )
//      .build()
//
//  "Routes" should {
//    "send 404 for request of unknown resource" in {
//      status(fakeRequest("/abcdef")) mustBe NOT_FOUND
//    }
//  }
//
//  "HealthController" should {
//    "get the health status" in {
//      val health = fakeRequest("/health")
//      status(health) mustBe OK
//      contentType(health) mustBe Some("application/json")
//      contentAsString(health) must include("uptime")
//      contentAsString(health) must include("startTime")
//    }
//  }
//
//  "HomeController" should {
//    "render default app route" in {
//      val home = fakeRequest("/")
//      // redirect
//      status(home) mustEqual SEE_OTHER
//      val res = getValue(redirectLocation(home))
//      res must include("/health")
//      flash(home).get("status") mustBe Some("ok")
//    }
//
//    "display swagger documentation" in {
//      val docs = fakeRequest("/docs")
//      status(docs) mustEqual SEE_OTHER
//      val res = getValue(redirectLocation(docs))
//      res must include("/swagger-ui/index.html")
//      contentAsString(docs) mustNot include("Not_FOUND")
//    }
//  }
//
//  // TODO -  Add new test for new routes
//  "AdminDataController" should {
//    "return 400 when an incorrect period format is used" in {
//      val search = fakeRequest("/v1/records/12345/periods/1706")
//      status(search) mustBe BAD_REQUEST
//      contentType(search) mustBe Some("application/json")
//      contentAsString(search) must include("Invalid period")
//    }
//
//    "return 404 when a record cannot be found" in {
//      val search = fakeRequest("/v1/records/99999/periods/201706")
//      status(search) mustBe NOT_FOUND
//      contentAsString(search) must include("Could not find record")
//    }
//
//    "return 200 when a record is found for a specified period" in {
//      val id = "03007252"
//      val period = "201706"
//      val search = fakeRequest(s"/v1/records/$id/periods/$period")
//      status(search) mustBe OK
//      (contentAsJson(search) \ "id").as[String] mustBe id
//      (contentAsJson(search) \ "period").as[String] mustBe period
//      (contentAsJson(search) \ "variables" \ "companynumber").as[String] mustBe id
//    }
//  }
//
//  private[this] def fakeRequest(uri: String, method: String = GET) =
//    route(app, FakeRequest(method, uri)).getOrElse(sys.error(s"Can not find route $uri."))
//}
