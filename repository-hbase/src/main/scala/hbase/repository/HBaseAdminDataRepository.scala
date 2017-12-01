package hbase.repository

import javax.inject.Inject

import scala.collection.JavaConversions._
import scala.concurrent.{ ExecutionContext, Future }

import org.apache.hadoop.hbase.client.{ Result, _ }
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.{ CellUtil, TableName }
import org.slf4j.{ Logger, LoggerFactory }
import com.github.nscala_time.time.Imports.{ DateTimeFormat, YearMonth }

import hbase.connector.HBaseConnector
import hbase.util.RowKeyUtils
import hbase.util.RowKeyUtils.REFERENCE_PERIOD_FORMAT
//import hbase.table.TableNames
import scala.util.{ Failure, Success, Try }

import com.typesafe.config.{ Config, ConfigFactory }

import model.AdminData
// import scala.concurrent.ExecutionContext.Implicits.global

/**
 * HBaseAdminDataRepository
 * ----------------
 * Author: haqa
 * Date: 20 November 2017 - 11:58
 * Copyright (c) 2017  Office for National Statistics
 */
class HBaseAdminDataRepository @Inject() (
    val connector: HBaseConnector
) extends AdminDataRepository {

  implicit val ec = ExecutionContext.global

  private final val tableName: TableName = TableName.valueOf(
    System.getProperty("hbase.namespace", ""),
    System.getProperty("hbase.table", "data")
  )

  private final val config: Config = ConfigFactory.load()
  private final val logger: Logger = LoggerFactory.getLogger(getClass.getName)
  private final val HARDCODED_CURRENT_PERIOD: YearMonth = YearMonth.parse("201706", DateTimeFormat.forPattern(REFERENCE_PERIOD_FORMAT))
  private final val OPEN_ALERT = "----- circuit breaker opened! -----"
  private final val CLOSED_ALERT = "----- circuit breaker closed! -----"
  private final val HALF_OPEN_ALERT = "----- circuit breaker half-open -----"

  //  private final val system = ActorSystem("hbase-repo-circuit-breaker")
  //  implicit val exc: ExecutionContextExecutor = system.dispatcher
  //
  //  private final val circuitBreaker: CircuitBreaker =
  //    new CircuitBreaker(
  //      system.scheduler,
  //      maxFailures = config.getInt("failure.threshold"),
  //      callTimeout = config.getInt("failure.declaration.time") seconds,
  //      resetTimeout = config.getInt("reset.timeout") seconds
  //    ).onOpen(logger.info(OPEN_ALERT))
  //      .onClose(logger.warn(CLOSED_ALERT))
  //      .onHalfOpen(logger.warn(HALF_OPEN_ALERT))
  //
  //  implicit val timeout = Timeout(2 seconds)

  // @TODO - fix + add circuit-breaker
  override def lookup(referencePeriod: Option[YearMonth], key: String): Future[Option[AdminData]] = Future.successful(getAdminData(referencePeriod, key))

  override def getCurrentPeriod: Future[YearMonth] = Future.successful(HARDCODED_CURRENT_PERIOD)

  @throws(classOf[Exception])
  private def getAdminData(referencePeriod: Option[YearMonth], key: String): Option[AdminData] = {
    val rowKey = referencePeriod match {
      case (Some(r)) =>
        RowKeyUtils.createRowKey(r, key)
      case None =>
        RowKeyUtils.createRowKey(HARDCODED_CURRENT_PERIOD, key)
    }
    val endRowKey = createEndRowKey(key)
    val maxResult = 1L
    Try(connector.getConnection.getTable(tableName)) match {
      case Success(table: Table) =>
        val scan = new Scan()
          .setStartRow(Bytes.toBytes(endRowKey))
          .setStopRow(Bytes.toBytes(key))
          .setReversed(true)
          .setMaxVersions(maxResult.toInt)
        scan.setMaxResultSize(maxResult)

        //        Option(table.getScanner(scan).next) match {
        //          case Some(rec) =>
        //            logger.debug("Found data for row key '{}'", rowKey)
        //            Some(convertToAdminData(rec))
        //          case None =>
        //            logger.debug("No data found for row key '{}'", rowKey)
        //            None
        //        }
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

  // @TODO - Fix
  private def createEndRowKey(key: String): String = {
    val l = (key.last.toLong + 1).toChar
    val p = key.substring(0, key.length() - 1) + s"$l"
    p
  }

}
