package hbase.repository

import javax.inject.Inject

import scala.collection.JavaConversions._
import scala.concurrent.{ ExecutionContext, ExecutionContextExecutor, Future }
import scala.util.{ Failure, Success, Try }

import play.api.Configuration
import org.apache.hadoop.hbase.CellUtil
import org.apache.hadoop.hbase.client.{ Get, Result, Scan, Table }
import org.apache.hadoop.hbase.util.Bytes
import com.github.nscala_time.time.Imports.YearMonth

import hbase.connector.HBaseConnector
import hbase.model.AdminData
import hbase.repository.AdminDataRepository.LOGGER
import hbase.util.{ HBaseConfigProperties, RowKeyUtils }

/**
 * InMemoryAdminDataRepository
 * ----------------
 * Author: haqa
 * Date: 11 January 2018 - 11:46
 * Copyright (c) 2017  Office for National Statistics
 */

class InMemoryAdminDataRepository @Inject() (val connector: HBaseConnector, val configuration: Configuration)
  extends AdminDataRepository with HBaseConfigProperties {

  private val ROWKEY_UTILS = new RowKeyUtils()
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  // TODO - add Circuitbreaker
  override def lookup(referencePeriod: Option[YearMonth], key: String, max: Option[Long]): Future[Option[Seq[AdminData]]] = Future.successful(getAdminData(referencePeriod, key, max))

  //@ TODO - reduce complexity -> sub method inner cases
  @throws(classOf[Throwable])
  private def getAdminData(referencePeriod: Option[YearMonth], key: String, max: Option[Long]): Option[Seq[AdminData]] = {
    Try(connector.getConnection.getTable(tableName)) match {
      case Success(table: Table) =>
        referencePeriod match {
          case Some(yearMonth: YearMonth) =>
            getRestRequest(table, yearMonth, key)
          case None =>
            scanRestRequest(table, key, max)
        }
      case Failure(e: Throwable) =>
        LOGGER.error(s"Error getting data for row key $key", e)
        throw e
    }
  }

  private def getRestRequest(table: Table, referencePeriod: YearMonth, key: String): Option[Seq[AdminData]] = {
    val rowKey = ROWKEY_UTILS.createRowKey(referencePeriod, key, reverseFlag)
    table.get(new Get(Bytes.toBytes(rowKey))) match {
      case res if res.isEmpty =>
        LOGGER.debug("No data found for row key '{}'", rowKey)
        None
      case result: Result =>
        LOGGER.debug("Found data for row key '{}'", rowKey)
        Some(Seq(convertToAdminData(result)))
    }
  }

  private def scanRestRequest(table: Table, key: String, max: Option[Long]): Option[Seq[AdminData]] = {
    val scan = new Scan()
      .setReversed(true)
      .setRowPrefixFilter(Bytes.toBytes(key + ROWKEY_UTILS.DELIMITER))
    if (max.isDefined) {
      scan.setMaxResultSize(max.get)
    }
    Option(table.getScanner(scan).next) match {
      // TODO - use type check
      case Some(_: Result) =>
        //      case Some(_) =>
        LOGGER.debug("Found data for prefix row key '{}'", key)
        Some(table.getScanner(scan).map { x => convertToAdminData(x) }.toSeq)
      case None =>
        LOGGER.debug("No data found for prefix row key '{}'", key)
        None
    }
  }

  private def convertToAdminData(result: Result): AdminData = {
    val adminData: AdminData = ROWKEY_UTILS.createAdminDataFromRowKey(Bytes.toString(result.getRow), reverseFlag)
    val varMap = result.listCells.toList.map { cell =>
      val column = new String(CellUtil.cloneQualifier(cell))
      val value = new String(CellUtil.cloneValue(cell))
      column -> value
    }.toMap
    val newPutAdminData = adminData.putVariable(varMap)
    newPutAdminData
  }

}
