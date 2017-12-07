package hbase

import java.io.IOException

import org.apache.hadoop.hbase.CellUtil
import org.apache.hadoop.hbase.HConstants
import org.apache.hadoop.hbase.util.Bytes.toBytes
import org.apache.hadoop.hbase.KeyValue
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.util.Bytes

import scala.concurrent.Await
import scala.reflect.ClassTag
import scala.collection.JavaConverters._
import com.github.nscala_time.time.Imports.YearMonth
import org.joda.time.format.DateTimeFormat
import org.scalatest.mockito.MockitoSugar
import org.junit.Assert.assertEquals
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.{ FlatSpec, Matchers }
import hbase.connector.HBaseConnector
import hbase.repository.HBaseAdminDataRepository
import hbase.util.RowKeyUtils
import hbase.model.AdminData
import play.api.inject.guice.GuiceApplicationBuilder
import services.websocket.RequestGenerator

import scala.concurrent.duration._

/**
 * Created by coolit on 07/12/2017.
 */

// For some reason, when running the test with a normal @Inject, it won't work
// So using this workaround for now
trait Inject {
  lazy val injector = (new GuiceApplicationBuilder).injector()

  def inject[T: ClassTag]: T = injector.instanceOf[T]
}

class HBaseAdminDataRepositoryScalaTest extends FlatSpec with MockitoSugar with Matchers with Inject {
  lazy val ws = inject[RequestGenerator]

  val dateFormat = AdminData.REFERENCE_PERIOD_FORMAT

  val connector = mock[HBaseConnector]
  val connection = mock[Connection]
  val table = mock[Table]
  val result = mock[Result]
  val resultScanner = mock[ResultScanner]

  def setup =
    new {
      val repository = new HBaseAdminDataRepository(connector, ws)
      when(connector.getConnection()) thenReturn (connection)
      when(connection.getTable(any())) thenReturn (table)
      when(table.get(any[Get])) thenReturn (result)
      when(table.getScanner(any[Scan])) thenReturn (resultScanner)
      when(resultScanner.next()) thenReturn (result)
    }

  private def createRowKey(period: YearMonth, id: String) = RowKeyUtils.createRowKey(period, id)

  "repository.getCurrentPeriod()" should "return the current period" in {
    val s = setup
    val dateString = "201706"
    val date = YearMonth.parse(dateString, DateTimeFormat.forPattern(dateFormat))
    val period = Await.result(s.repository.getCurrentPeriod, 1 second)
    assertEquals(period, date)
  }

  "repository.lookup()" should "return a valid result" in {
    val s = setup
    val columnFamily = toBytes("d")

    // Test data
    val testId = "12345"
    val testPeriod = YearMonth.parse("200812", DateTimeFormat.forPattern(dateFormat))
    val companyName = "My Company"

    // Create cells for each column
    val rowKey = RowKeyUtils.createRowKey(testPeriod, testId)
    val nameCell = CellUtil.createCell(Bytes.toBytes(rowKey), columnFamily, Bytes.toBytes("name"), 9223372036854775807L, KeyValue.Type.Maximum, Bytes.toBytes(companyName), HConstants.EMPTY_BYTE_ARRAY)
    val cells = List(nameCell).asJava

    when(result.isEmpty()) thenReturn (false)
    when(result.listCells()) thenReturn (cells)
    when(result.getRow()) thenReturn (Bytes.toBytes(rowKey))

    val lookup = Await.result(s.repository.lookup(Some(testPeriod), testId), 1 second).getOrElse(throw new Exception("Unable to do repository lookup"))
    assertEquals(lookup.id, testId)
    assertEquals(lookup.referencePeriod, testPeriod)
    assertEquals(lookup.variables.getOrElse("name", throw new Exception("Unable to get company name")), companyName)
  }

  //  "repository.lookupOvertime()" should "return all the results relating to a particular id" in {
  //    val s = setup
  //    val columnFamily = toBytes("d")
  //
  //    // Test data
  //    val testId = "12345"
  //    val testPeriod1 = YearMonth.parse("200812", DateTimeFormat.forPattern(dateFormat))
  //    val testPeriod2 = YearMonth.parse("200811", DateTimeFormat.forPattern(dateFormat))
  //    val testPeriod3 = YearMonth.parse("200810", DateTimeFormat.forPattern(dateFormat))
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
  //    val nameCell1 = CellUtil.createCell(Bytes.toBytes(rowKey1), columnFamily, Bytes.toBytes("name"), 9223372036854775807L, KeyValue.Type.Maximum, Bytes.toBytes(companyName1), HConstants.EMPTY_BYTE_ARRAY)
  //    val nameCell2 = CellUtil.createCell(Bytes.toBytes(rowKey2), columnFamily, Bytes.toBytes("name"), 9223372036854775807L, KeyValue.Type.Maximum, Bytes.toBytes(companyName2), HConstants.EMPTY_BYTE_ARRAY)
  //    val nameCell3 = CellUtil.createCell(Bytes.toBytes(rowKey3), columnFamily, Bytes.toBytes("name"), 9223372036854775807L, KeyValue.Type.Maximum, Bytes.toBytes(companyName3), HConstants.EMPTY_BYTE_ARRAY)
  //    val cells = List(nameCell1, nameCell2, nameCell3).asJava
  //
  //    when(result.isEmpty()) thenReturn (false)
  //    when(result.listCells()) thenReturn (cells)
  //    when(result.getRow()) thenReturn (Bytes.toBytes(rowKey1), Bytes.toBytes(rowKey2), Bytes.toBytes(rowKey3))
  //
  //    val lookup = Await.result(s.repository.lookupOvertime(testId, Some(3L)), 1 second).getOrElse(throw new Exception("Unable to do repository lookup"))
  //    // Need to test that it returns a list of results
  //    // Also test when we use 1L it only returns 1 result
  //  }

  "repository.lookup()" should "return an empty result if no record with that id is present" in {
    val s = setup
    val testPeriod = YearMonth.parse("201706", DateTimeFormat.forPattern(dateFormat))
    when(result.isEmpty()) thenReturn (true)
    val lookup = Await.result(s.repository.lookup(Some(testPeriod), "12345"), 1 second)
    assertEquals(lookup, None)
  }

  "repository.lookup()" should "throw an exception" in {
    val s = setup
    val testPeriod = YearMonth.parse("201706", DateTimeFormat.forPattern(dateFormat))
    when(connection.getTable(any())) thenThrow (new IOException("Failed to retrieve data"))
    assertThrows[IOException] {
      Await.result(s.repository.lookup(Some(testPeriod), "12345"), 1 second)
    }
  }
}
