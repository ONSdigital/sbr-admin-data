package hbase.repository

import javax.inject.Inject

import com.github.nscala_time.time.Imports.YearMonth
import com.netaporter.uri.dsl._
import hbase.model.AdminData
import hbase.repository.AdminDataRepository._
import hbase.util.{ HBaseConfig, RowKeyUtils }
import play.api.Configuration
import play.api.http.{ ContentTypes, Status }
import play.api.libs.json.JsValue
import play.api.mvc.Results
import services.util.EncodingUtil.{ decodeBase64, encodeBase64 }
import services.websocket.RequestGenerator

import scala.concurrent.{ ExecutionContext, ExecutionContextExecutor, Future }
import scala.util.{ Failure, Success, Try }

/**
 * RestAdminDataRepository
 * ----------------
 * Author: haqa
 * Date: 11 January 2018 - 11:45
 * Copyright (c) 2017  Office for National Statistics
 */
class RestAdminDataRepository @Inject() (ws: RequestGenerator, val configuration: Configuration)
  extends AdminDataRepository with HBaseConfig with Status with Results with ContentTypes {

  implicit val ec: ExecutionContextExecutor = ExecutionContext.global
  private val AUTH = encodeBase64(Seq(username, password))
  private val HEADERS = Seq("Accept" -> "application/json", "Authorization" -> s"Basic $AUTH")

  // TODO - add Circuit breaker
  override def lookup(referencePeriod: Option[YearMonth], key: String, max: Option[Long]): Future[Option[Seq[AdminData]]] = getAdminData(referencePeriod, key, max)

  @throws(classOf[Throwable])
  private def getAdminData(referencePeriod: Option[YearMonth], key: String, max: Option[Long]): Future[Option[Seq[AdminData]]] = {
    (referencePeriod match {
      case Some(r: YearMonth) =>
        val rowKey = RowKeyUtils.createRowKey(r, key)
        val uri = baseUrl / tableName.getNameWithNamespaceInclAsString / rowKey / columnFamily
        LOGGER.debug(s"Making restful GET request to HBase with url path ${uri.toString} " +
          s"and headers ${HEADERS.head.toString}")
        ws.singleGETRequest(uri.toString, HEADERS)
      case None =>
        /**
         * @note - UNCOMMENT for HBase Rest support on Cloudera ch.6 release
         *       allowing reverser and thereby limit to work
         *
         * val params = if (max.isDefined) {
         *  Seq("reversed" -> "true", "limit" -> max.get.toString)
         * } else {
         *  Seq("reversed" -> "true")
         * }
         */
        val uri = baseUrl / tableName.getNameWithNamespaceInclAsString / key + RowKeyUtils.DELIMITER + "*"
        LOGGER.debug(s"Making restful SCAN request to HBase with url ${uri.toString}, " +
          s"headers ${HEADERS.head.toString} ")
        ws.singleGETRequest(uri.toString, HEADERS)
    }).map {
      case response if response.status == OK => {
        val defaultGetLimit: Int = 1
        val resp = (response.json \ "Row").as[Seq[JsValue]]
        Try(resp.map(v => convertToAdminData(v))) match {
          case Success(Seq()) =>
            LOGGER.debug("No data found for prefix row key '{}'", key)
            None
          case Success(adminData: Seq[AdminData]) =>
            LOGGER.debug("Found data for prefix row key '{}'", key)
            /**
             * @note - UNCOMMENT when Cloudera HBase REST for reverse and remove application
             *       side reverse and limit.
             *
             *       Some(adminData)
             *
             * All responses need to be in DESC and capped - GET is LIMIT 1 thus results to no effect
             */
            Some(adminData.reverse.take(max.getOrElse(defaultGetLimit).toString.toInt))
          case Failure(e: Throwable) =>
            LOGGER.error(s"Error getting data for row key $key", e)
            throw e
        }
      }
    }
  }

  private def convertToAdminData(result: JsValue): AdminData = {
    val columnFamilyAndValueSubstring = 2
    val key = (result \ "key").as[String]
    LOGGER.debug(s"Found record with $key")
    val adminData: AdminData = RowKeyUtils.createAdminDataFromRowKey(decodeBase64(key))
    val varMap = (result \ "Cell").as[Seq[JsValue]].map { cell =>
      val column = decodeBase64((cell \ "column").as[String]).split(":", columnFamilyAndValueSubstring).last
      val value = decodeBase64((cell \ "$").as[String])
      column -> value
    }.toMap
    val newPutAdminData = adminData.putVariable(varMap)
    newPutAdminData
  }

}
