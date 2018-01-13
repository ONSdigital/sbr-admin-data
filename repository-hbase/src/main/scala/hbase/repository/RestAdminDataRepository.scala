package hbase.repository

import javax.inject.Inject

import scala.concurrent.{ ExecutionContext, ExecutionContextExecutor, Future }
import scala.util.{ Failure, Success, Try }

import play.api.http.{ ContentTypes, Status }
import play.api.libs.json.JsValue
import play.api.mvc.Results
import com.github.nscala_time.time.Imports.YearMonth
import com.netaporter.uri.dsl._

import hbase.model.AdminData
import hbase.repository.AdminDataRepository._
import hbase.util.HBaseConfig.{ baseUrl, password, tableName, username, _ }
import hbase.util.RowKeyUtils
import services.util.EncodingUtil.{ decodeBase64, encodeBase64 }
import services.websocket.RequestGenerator

/**
 * RestAdminDataRepository
 * ----------------
 * Author: haqa
 * Date: 11 January 2018 - 11:45
 * Copyright (c) 2017  Office for National Statistics
 */
class RestAdminDataRepository @Inject() (ws: RequestGenerator) extends AdminDataRepository
  with Status with Results with ContentTypes {

  implicit val ec: ExecutionContextExecutor = ExecutionContext.global
  private val auth = encodeBase64(Seq(username, password))

  override def lookup(referencePeriod: Option[YearMonth], key: String, max: Long = MAX_RESULT_SIZE): Future[Option[Seq[AdminData]]] =
    getAdminData(referencePeriod, key, max)

  @throws(classOf[Throwable])
  private def getAdminData(referencePeriod: Option[YearMonth], key: String, max: Long): Future[Option[Seq[AdminData]]] = {
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
            Some(adminData)
          case Failure(e: Throwable) =>
            LOGGER.error(s"Error getting data for row key $key", e)
            throw e
        }
      }
      case response if response.status == NOT_FOUND => None
    }
  }

  private def convertToAdminData(result: JsValue): AdminData = {
    val key = (result \ "key").as[String]
    LOGGER.debug(s"Found record $key")
    val adminData: AdminData = RowKeyUtils.createAdminDataFromRowKey(decodeBase64(key))
    val varMap = (result \ "Cell").as[Seq[JsValue]].map { cell =>
      val column = decodeBase64((cell \ "column").as[String])
      val value = decodeBase64((cell \ "$").as[String])
      column -> value
    }.toMap
    val newPutAdminData = adminData.putVariable(varMap)
    newPutAdminData
  }

}
