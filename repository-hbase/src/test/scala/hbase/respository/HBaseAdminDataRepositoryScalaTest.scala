package hbase.respository

import java.io.IOException

import com.github.nscala_time.time.Imports.YearMonth
import com.typesafe.config.ConfigFactory
import hbase.connector.HBaseConnector
import hbase.model.AdminData
import hbase.repository.InMemoryAdminDataRepository
import hbase.util.RowKeyUtils
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.util.Bytes.toBytes
import org.apache.hadoop.hbase.{ CellUtil, HConstants, KeyValue }
import org.joda.time.format.DateTimeFormat
import org.junit.Assert.assertEquals
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{ FlatSpec, Matchers }
import play.api.Configuration
import services.websocket.RequestGenerator

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration._

class HBaseAdminDataRepositoryScalaTest extends FlatSpec with MockitoSugar with Matchers {

  private val MAX_RESULT_SIZE: Long = 12L
  private val dateFormat = AdminData.REFERENCE_PERIOD_FORMAT

  private val connector = mock[HBaseConnector]
  private val connection = mock[Connection]
  private val table = mock[Table]
  private val result = mock[Result]
  private val resultScanner = mock[ResultScanner]
  private val ws = mock[RequestGenerator]

  private def createRowKey(period: YearMonth, id: String) = RowKeyUtils.createRowKey(period, id)

  def setup =
    new {
      val repository = new InMemoryAdminDataRepository(connector, Configuration(ConfigFactory.load()))
      when(connector.getConnection) thenReturn connection
      when(connection.getTable(any())) thenReturn table
      when(table.get(any[Get])) thenReturn result
      when(table.getScanner(any[Scan])) thenReturn resultScanner
      when(resultScanner.next()) thenReturn result
    }

  //  "repository.getCurrentPeriod()" should "return the current period" in {
  //    val s = setup
  //    val dateString = "201706"
  //    val date = YearMonth.parse(dateString, DateTimeFormat.forPattern(dateFormat))
  //    val period = Await.result(s.repository.getCurrentPeriod, 1 second)
  //    assertEquals(period, date)
  //  }

  "repository.lookup()" should "return a valid result" in {
    val s = setup
    val columnFamily = toBytes("d")

    // Test data
    val testId = "12345"
    val testPeriod = YearMonth.parse("200812", DateTimeFormat.forPattern(dateFormat))
    val companyName = "My Company"

    // Create cells for each column
    val rowKey = RowKeyUtils.createRowKey(testPeriod, testId)
    val nameCell = CellUtil.createCell(Bytes.toBytes(rowKey), columnFamily, Bytes.toBytes("name"),
      9223372036854775807L, KeyValue.Type.Maximum, Bytes.toBytes(companyName), HConstants.EMPTY_BYTE_ARRAY)
    val cells = List(nameCell).asJava

    when(result.isEmpty) thenReturn false
    when(result.listCells()) thenReturn cells
    when(result.getRow) thenReturn Bytes.toBytes(rowKey)

    val lookup = Await.result(s.repository.lookup(Some(testPeriod), testId, Some(MAX_RESULT_SIZE)), 1 second)
      .getOrElse(throw new Exception("Unable to do repository lookup"))
    assertEquals(lookup.head.id, testId)
    assertEquals(lookup.head.referencePeriod, testPeriod)
    assertEquals(
      lookup.head.variables.getOrElse("name", throw new Exception("Unable to get company name")),
      companyName)
  }

  //  "repository.lookupOvertime()" should "return all the results relating to a particular id" in {
  //    val s = setup
  //    val columnFamily = toBytes("d")
  //
  //    // Test data
  //    val testId = "12345"
  //    val testPeriod1 = YearMonth.parse("200812", DateTimeFormat.forPattern(dateFormat))
  //
  //    val rowKey1 = createRowKey(testPeriod1, testId)
  //    val rowKey2 = createRowKey(testPeriod1, testId)
  //    val rowKey3 = createRowKey(testPeriod1, testId)
  //
  //    val companyName1 = "name 200812"
  //    val companyName2 = "name 200812"
  //    val companyName3 = "name 200812"
  //
  //    // Create cells for each column
  //    val nameCell1 = CellUtil.createCell(Bytes.toBytes(rowKey1), columnFamily, Bytes.toBytes("name"),
  //      9223372036854775807L, KeyValue.Type.Maximum, Bytes.toBytes(companyName1), HConstants.EMPTY_BYTE_ARRAY)
  //    val nameCell2 = CellUtil.createCell(Bytes.toBytes(rowKey2), columnFamily, Bytes.toBytes("name"),
  //      9223372036854775807L, KeyValue.Type.Maximum, Bytes.toBytes(companyName2), HConstants.EMPTY_BYTE_ARRAY)
  //    val nameCell3 = CellUtil.createCell(Bytes.toBytes(rowKey3), columnFamily, Bytes.toBytes("name"),
  //      9223372036854775807L, KeyValue.Type.Maximum, Bytes.toBytes(companyName3), HConstants.EMPTY_BYTE_ARRAY)
  //    val cells = List(nameCell1, nameCell2, nameCell3).asJava
  //
  //    when(result.isEmpty) thenReturn false
  //    when(result.listCells()) thenReturn cells
  //    when(result.getRow) thenReturn (Bytes.toBytes(rowKey1), Bytes.toBytes(rowKey2), Bytes.toBytes(rowKey3))
  //
  //    Await.result(s.repository.lookupOvertime(testId, Some(3L)), 1 second).getOrElse(throw new Exception("Unable to do repository lookup"))
  //    // Need to test that it returns a list of results
  //    // Also test when we use 1L it only returns 1 result
  //  }

  "repository.lookup()" should "return an empty result if no record with that id is present" in {
    val s = setup
    val testPeriod = YearMonth.parse("201706", DateTimeFormat.forPattern(dateFormat))
    when(result.isEmpty) thenReturn true
    val lookup = Await.result(s.repository.lookup(Some(testPeriod), "12345", None), 1 second)
    assertEquals(lookup, None)
  }

  "repository.lookup()" should "throw an exception" in {
    val s = setup
    val testPeriod = YearMonth.parse("201706", DateTimeFormat.forPattern(dateFormat))
    when(connection.getTable(any())) thenThrow new IOException("Failed to retrieve data")
    assertThrows[IOException] {
      Await.result(s.repository.lookup(Some(testPeriod), "12345", None), 1 second)
    }
  }
}
