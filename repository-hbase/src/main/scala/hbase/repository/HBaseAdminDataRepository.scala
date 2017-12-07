package hbase.repository

import javax.inject.Inject

import scala.collection.JavaConversions._
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }

import play.api.http.Status
import play.api.libs.ws.WSResponse
import org.apache.hadoop.hbase.CellUtil
import org.apache.hadoop.hbase.client.{ Result, _ }
import org.apache.hadoop.hbase.util.Bytes
import org.slf4j.{ Logger, LoggerFactory }
import com.github.nscala_time.time.Imports.{ DateTimeFormat, YearMonth }
import com.google.common.base.Charsets
import com.google.common.io.BaseEncoding
import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._

import hbase.connector.HBaseConnector
import hbase.model.AdminData
import hbase.util.HBaseConfig._
import hbase.util.RowKeyUtils
import hbase.util.RowKeyUtils.REFERENCE_PERIOD_FORMAT
import services.websocket.RequestGenerator

/**
 * HBaseAdminDataRepository
 * ----------------
 * Author: haqa
 * Date: 20 November 2017 - 11:58
 * Copyright (c) 2017  Office for National Statistics
 */
class HBaseAdminDataRepository @Inject() (
    val connector: HBaseConnector,
    ws: RequestGenerator
) extends AdminDataRepository with Status {

  implicit val ec = ExecutionContext.global

  private final val logger: Logger = LoggerFactory.getLogger(getClass.getName)
  private final val HARDCODED_CURRENT_PERIOD: YearMonth = YearMonth.parse("201706", DateTimeFormat.forPattern(REFERENCE_PERIOD_FORMAT))
  private final val OPEN_ALERT = "----- circuit breaker opened! -----"
  private final val CLOSED_ALERT = "----- circuit breaker closed! -----"
  private final val HALF_OPEN_ALERT = "----- circuit breaker half-open -----"

  private val auth = BaseEncoding.base64.encode(s"$username:$password".getBytes(Charsets.UTF_8))

  // @TODO - fix + add circuit-breaker
  override def lookup(referencePeriod: Option[YearMonth], key: String): Future[Option[AdminData]] = Future.successful(getAdminData(referencePeriod, key))
  def lookupOvertime(key: String, periodRange: Option[Long] = Some(12L)): Future[Option[AdminData]] = Future.successful(getAdminDataOverTime(key, periodRange.getOrElse(12L)))

  override def getCurrentPeriod: Future[YearMonth] = Future.successful(HARDCODED_CURRENT_PERIOD)

  override def lookupRest(key: String, referencePeriod: YearMonth): Future[WSResponse] = getAdminDataRest(key, referencePeriod)

  @throws(classOf[Exception])
  private def getAdminData(referencePeriod: Option[YearMonth] = Some(HARDCODED_CURRENT_PERIOD), key: String): Option[AdminData] = {
    val rowKey = RowKeyUtils.createRowKey(referencePeriod.getOrElse(HARDCODED_CURRENT_PERIOD), key)
    Try(connector.getConnection.getTable(tableName)) match {
      case Success(table: Table) =>
        val scan = new Scan()
          .setReversed(true)
          .setRowPrefixFilter(Bytes.toBytes(key))
        scan.setMaxResultSize(2)
        table.get(new Get(Bytes.toBytes(rowKey))) match {
          case res if res.isEmpty =>
            logger.debug("No data found for row key '{}'", rowKey)
            None
          case result =>
            logger.debug("Found data for row key '{}'", rowKey)
            Some(convertToAdminData(result))
        }
      case Failure(e: Exception) =>
        logger.error(s"Error getting data for row key $rowKey", e)
        throw e
    }
  }

  @throws(classOf[Exception])
  private def getAdminDataRest(key: String, referencePeriod: YearMonth): Future[WSResponse] = {
    val rowKey = RowKeyUtils.createRowKey(referencePeriod, key)
    val url: Uri = baseUrl / tableName.getNameWithNamespaceInclAsString / rowKey / columnFamily
    logger.debug(s"sending ws request to ${url.toString}")
    val headers = Seq("Accept" -> "application/json", "Authorization" -> s"Basic $auth")
    val request = ws.singleGETRequest(url.toString, headers)
    request

  }

  @throws(classOf[Exception])
  private def getAdminDataOverTime(key: String, periodRange: Long): Option[AdminData] = {
    val cf = Bytes.toBytes("d")
    val prefix = Bytes.toBytes(key)
    Try(connector.getConnection.getTable(tableName)) match {
      case Success(table: Table) =>
        val scan = new Scan(prefix)
          .setRowPrefixFilter(prefix)
          .setReversed(true)
          .addFamily(cf)
        Try(table.getScanner(scan)) match {
          case Success(result: ResultScanner) =>
            logger.debug("Found data for row key '{}'", key)
            Some(convertToAdminData(result.next))
          case Failure(_) =>
            logger.debug("No data found for row key '{}'", key)
            None
        }
      case Failure(e: Exception) =>
        logger.error(s"Error getting data for row key $key", e)
        throw e
    }
  }

  //  private def convertToAdminData(result: JsValue): AdminData = {
  //    val adminData: AdminData = RowKeyUtils.createAdminDataFromRowKey((result \ "Row" \ "Key").as[String])
  //    val varMap = (result \ "Row" \ "Key" \ "Cell").map
  //    val varMap = result.listCells.toList.map { cell =>
  //      val column = new String(CellUtil.cloneQualifier(cell))
  //      val value = new String(CellUtil.cloneValue(cell))
  //      logger.debug(s"Found data column $column with value $value")
  //      column -> value
  //    }.toMap
  //    val newPutAdminData = adminData.putVariable(varMap)
  //    newPutAdminData
  //  }

  private def convertToAdminData(result: Result): AdminData = {
    val adminData: AdminData = RowKeyUtils.createAdminDataFromRowKey(Bytes.toString(result.getRow))
    val varMap = result.listCells.toList.map { cell =>
      val column = new String(CellUtil.cloneQualifier(cell))
      val value = new String(CellUtil.cloneValue(cell))
      logger.debug(s"Found data column $column with value $value")
      column -> value
    }.toMap
    val newPutAdminData = adminData.putVariable(varMap)
    newPutAdminData
  }

}

