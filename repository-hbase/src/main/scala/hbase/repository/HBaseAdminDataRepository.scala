package hbase.repository

import javax.inject.Inject

import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

import play.api.http.{ContentTypes, Status}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Results, Result => PlayResult}
import org.apache.hadoop.hbase.CellUtil
import org.apache.hadoop.hbase.client.{Result, _}
import org.apache.hadoop.hbase.util.Bytes
import org.slf4j.{Logger, LoggerFactory}
import com.github.nscala_time.time.Imports.{DateTimeFormat, YearMonth}
import com.google.common.base.Charsets
import com.google.common.io.BaseEncoding
import com.netaporter.uri.dsl._

import hbase.connector.HBaseConnector
import hbase.model.AdminData
import hbase.util.HBaseConfig._
import hbase.util.RowKeyUtils
import hbase.util.RowKeyUtils.REFERENCE_PERIOD_FORMAT
import services.util.ResponseUtil.{decodeBase64, errAsJson}
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
  ws: RequestGenerator) extends AdminDataRepository with Status with Results with ContentTypes {

  implicit val ec = ExecutionContext.global

  private final val logger: Logger = LoggerFactory.getLogger(getClass.getName)
  private final val HARDCODED_CURRENT_PERIOD: YearMonth = YearMonth.parse("201706", DateTimeFormat.forPattern(REFERENCE_PERIOD_FORMAT))
  private final val OPEN_ALERT = "----- circuit breaker opened! -----"
  private final val CLOSED_ALERT = "----- circuit breaker closed! -----"
  private final val HALF_OPEN_ALERT = "----- circuit breaker half-open -----"

  private val maxResultSize: Long = 12L
  private val auth = BaseEncoding.base64.encode(s"$username:$password".getBytes(Charsets.UTF_8))
  //  private val auth = encodeBase64(Seq(username, password))

  // @TODO - fix + add circuit-breaker
  @deprecated("Migrated to other lookup", "12 Dec 2017 - feature/HBase-Rest")
  override def lookup(referencePeriod: Option[YearMonth], key: String): Future[Option[AdminData]] = Future.successful(getAdminData(referencePeriod, key))

  override def getCurrentPeriod: Future[YearMonth] = Future.successful(HARDCODED_CURRENT_PERIOD)

  override def lookup(key: String, referencePeriod: Option[YearMonth]): Future[PlayResult] = getAdminDataRest(key, referencePeriod)

  @deprecated("Migrated to getAdminDataRest", "12 Dec 2017 - feature/HBase-Rest")
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
  private def getAdminDataRest(key: String, referencePeriod: Option[YearMonth], max: Long = maxResultSize): Future[PlayResult] = {
    val headers = Seq("Accept" -> "application/json", "Authorization" -> s"Basic $auth")
    val request = referencePeriod match {
      case Some(r: YearMonth) =>
        val rowKey = RowKeyUtils.createRowKey(r, key)
        val uri = baseUrl / tableName.getNameWithNamespaceInclAsString / rowKey / columnFamily
        ws.singleGETRequest(uri.toString, headers)
      case None =>
        val params = Seq("reversed" -> "true", "limit" -> max.toString)
        val uri = baseUrl / tableName.getNameWithNamespaceInclAsString / key + "*"
        ws.singleGETRequest(uri.toString, headers, params)
    }
    request.map {
      case response if response.status == OK => {
        val resp = (response.json \ "Row").as[Seq[JsValue]]
        Try(resp.map(v => convertToAdminData(v))) match {
          case Success(adminData: Seq[AdminData]) =>
            //            Some(adminData)
            Ok(Json.toJson(adminData)).as(JSON)
          case Failure(ex) =>
            // TODO - add exception type
            //            None
            BadRequest(errAsJson(BAD_REQUEST, "bad_request", s"$ex"))
        }
      }
      case response if response.status == NOT_FOUND => NotFound(response.body).as(JSON)
    }
  }

  private def convertToAdminData(result: JsValue): AdminData = {
    val key = (result \ "key").as[String]
    logger.debug(s"Found record $key")
    val adminData: AdminData = RowKeyUtils.createAdminDataFromRowKey(decodeBase64(key))
    val varMap = (result \ "Cell").as[Seq[JsValue]].map { cell =>
      val column = decodeBase64((cell \ "column").as[String])
      val value = decodeBase64((cell \ "$").as[String])
      logger.debug(s"Found data column $column with value $value")
      column -> value
    }.toMap
    val newPutAdminData = adminData.putVariable(varMap)
    newPutAdminData
  }

  @deprecated("Migrated to convertToAdminData with JsValue param", "12 Dec 2017 - feature/HBase-Rest")
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

