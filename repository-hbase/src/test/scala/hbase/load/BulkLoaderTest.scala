package hbase.load

import java.io.File
import javax.inject.Inject

import scala.compat.java8.FutureConverters.toJava
import scala.concurrent.Future

import play.api.Configuration
import org.apache.hadoop.util.ToolRunner
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{ BeforeAndAfterAll, Matchers }
import com.github.nscala_time.time.Imports.YearMonth
import com.typesafe.config.ConfigFactory

import hbase.AbstractHBaseIT
import hbase.model.AdminData
import hbase.repository.HBaseAdminDataRepository

import services.websocket.RequestGenerator

/**
 * BulkLoaderTest
 * ----------------
 * Author: haqa
 * Date: 30 November 2017 - 09:44
 * Copyright (c) 2017  Office for National Statistics
 */

// @TODO - REMOVE NULLEXCEPTION methods
class BulkLoaderTest @Inject() (ws: RequestGenerator) extends AbstractHBaseIT with Matchers with MockitoSugar with BeforeAndAfterAll {

  private val TEST_CSV_ID_COLUMN = "1"
  private val TEST_CSV_HEADER_STRING = "entref"
  private val TEST_PERIOD: YearMonth = new YearMonth(2017, 6)
  private val TEST_PERIOD_STR = TEST_PERIOD.toString(AdminData.REFERENCE_PERIOD_FORMAT)
  private val TEST_CH_CSV = "test/resources/ch-data.csv"
  private val TEST_PAYE_CSV = "test/resources/paye-data.csv"
  private val TEST_VAT_CSV = "test/resources/vat-data.csv"

  @throws(classOf[Exception])
  private def setup = new {
    val bc = beforeClass
    val repository: HBaseAdminDataRepository = new HBaseAdminDataRepository(bc.HBASE_CONNECTOR, ws, Configuration(ConfigFactory.load()))
    val bulkLoader: BulkLoader = new BulkLoader(bc.HBASE_CONNECTOR)
  }

  /**
   * @throws NullPointerException
   * @throws Exception
   */
  it must "loadCompaniesData" in {
    val testSetup = setup

    val file = new File(TEST_CH_CSV)
    file should be('file) // a or an removed

    val result = loadData(Array[String](TABLE_NAME, TEST_PERIOD_STR, TEST_CH_CSV, TEST_CSV_ID_COLUMN, "companyname", ""))
    result should equal(0)

    val company: Future[Option[AdminData]] = testSetup.repository.lookup(Some(TEST_PERIOD), "04375380")

    toJava(company).toCompletableFuture.get.isDefined should equal(true)
    toJava(company).toCompletableFuture.get.getOrElse(throw new Exception("Null object found")).id should equal("04375380")
  }

  /**
   * @throws NullPointerException
   * @throws Exception
   */
  it must "loadPAYEData" in {
    val testSetup = setup

    val file = new File(TEST_PAYE_CSV);
    file should be('file)

    val result = loadData(Array[String](TABLE_NAME, TEST_PERIOD_STR, TEST_PAYE_CSV, TEST_CSV_ID_COLUMN, "entref", ""))
    result should equal(0)

    val payeReturn: Future[Option[AdminData]] = testSetup.repository.lookup(Some(TEST_PERIOD), "8878574")

    toJava(payeReturn).toCompletableFuture.get.isDefined should equal(true)
    toJava(payeReturn).toCompletableFuture.get.getOrElse(throw new Exception("Null object found")).id should equal("8878574")
  }

  /**
   * @throws NullPointerException
   * @throws Exception
   */
  it must "loadVATData" in {
    val testSetup = setup
    val file = new File(TEST_VAT_CSV)
    file should be('file)

    val result = loadData(Array[String](TABLE_NAME, TEST_PERIOD_STR, TEST_VAT_CSV, TEST_CSV_ID_COLUMN, "vatref", ""))
    result should equal(0)

    val vatReturn = testSetup.repository.lookup(Some(TEST_PERIOD), "808281648666")

    toJava(vatReturn).toCompletableFuture.get.isDefined should equal(true)
    toJava(vatReturn).toCompletableFuture.get.getOrElse(throw new Exception("Null object found")).id should equal("808281648666")

  }

  @throws(classOf[Exception])
  private def loadData(args: Array[String]) = {
    val testSetup = setup
    ToolRunner.run(testSetup.bc.HBASE_CONNECTOR.getConfiguration, testSetup.bulkLoader, args)
  }

}
