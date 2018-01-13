package hbase.repository

import javax.inject.Inject

import scala.collection.JavaConversions._
import scala.concurrent.{ ExecutionContext, ExecutionContextExecutor, Future }
import scala.util.{ Failure, Success, Try }
import org.apache.hadoop.hbase.CellUtil
import org.apache.hadoop.hbase.client.{ Get, Result, Scan, Table }
import org.apache.hadoop.hbase.util.Bytes
import com.github.nscala_time.time.Imports.YearMonth
import hbase.connector.HBaseConnector
import hbase.model.AdminData
import hbase.repository.AdminDataRepository._
import hbase.util.{ HBaseConfig, RowKeyUtils }
import play.api.Configuration

/**
 * InMemoryAdminDataRepository
 * ----------------
 * Author: haqa
 * Date: 11 January 2018 - 11:46
 * Copyright (c) 2017  Office for National Statistics
 */
class InMemoryAdminDataRepository @Inject() (val connector: HBaseConnector, val configuration: Configuration) extends AdminDataRepository with HBaseConfig {

  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  override def lookup(referencePeriod: Option[YearMonth], key: String, max: Long = MAX_RESULT_SIZE): Future[Option[Seq[AdminData]]] =
    Future.successful(getAdminData(referencePeriod, key, max))

  @throws(classOf[Throwable])
  private def getAdminData(referencePeriod: Option[YearMonth], key: String, max: Long): Option[Seq[AdminData]] = {
    Try(connector.getConnection.getTable(tableName)) match {
      case Success(table: Table) =>
        referencePeriod match {
          case Some(y: YearMonth) =>
            val rowKey = RowKeyUtils.createRowKey(referencePeriod.getOrElse(y), key)
            table.get(new Get(Bytes.toBytes(rowKey))) match {
              case res if res.isEmpty =>
                LOGGER.debug("No data found for row key '{}'", rowKey)
                None
              case result =>
                LOGGER.debug("Found data for row key '{}'", rowKey)
                Some(Seq(convertToAdminData(result)))
            }
          case None =>
            val scan = new Scan()
              .setReversed(true)
              .setRowPrefixFilter(Bytes.toBytes(key))
            scan.setMaxResultSize(max)
            Option(table.getScanner(scan).next) match {
              case Some(_) =>
                LOGGER.debug("Found data for prefix row key '{}'", key)
                Some(table.getScanner(scan).map { x => convertToAdminData(x) }.toSeq)
              case None =>
                LOGGER.debug("No data found for prefix row key '{}'", key)
                None
            }
        }
      case Failure(e: Throwable) =>
        LOGGER.error(s"Error getting data for row key $key", e)
        throw e
    }
  }

  private def convertToAdminData(result: Result): AdminData = {
    val adminData: AdminData = RowKeyUtils.createAdminDataFromRowKey(Bytes.toString(result.getRow))
    val varMap = result.listCells.toList.map { cell =>
      val column = new String(CellUtil.cloneQualifier(cell))
      val value = new String(CellUtil.cloneValue(cell))
      column -> value
    }.toMap
    val newPutAdminData = adminData.putVariable(varMap)
    newPutAdminData
  }

}
