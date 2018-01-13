package hbase.respository

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}

class HBaseRestAdminDataTest extends FlatSpec with Matchers with BeforeAndAfterEach{

  private val PORT = 8080
  private val HOST = "localhost"

  private val wireMockServer = new WireMockServer(wireMockConfig().port(PORT))

  override def beforeEach {
    wireMockServer.start()
    WireMock.configureFor(HOST, PORT)
  }

  override def afterEach {
    wireMockServer.stop()
  }


  "" should "" in {
    ???
  }

}
