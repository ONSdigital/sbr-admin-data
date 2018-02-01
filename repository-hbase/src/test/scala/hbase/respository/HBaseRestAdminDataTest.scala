package hbase.respository

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.scalatest.{ BeforeAndAfterEach, FlatSpec, Matchers }

class HBaseRestAdminDataTest extends FlatSpec with Matchers with BeforeAndAfterEach {

  private val PORT = 8080
  private val HOST = "localhost"

  private val WIRE_MOCK_SERVER = new WireMockServer(wireMockConfig().port(PORT))

  override def beforeEach {
    WIRE_MOCK_SERVER.start()
    WireMock.configureFor(HOST, PORT)
  }

  override def afterEach {
    WIRE_MOCK_SERVER.stop()
  }

  //  "" should "" in {
  //    ???
  //  }

}
