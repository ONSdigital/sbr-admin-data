package hbase.util

import play.api.Configuration
import org.apache.hadoop.hbase.TableName
import com.typesafe.config.Config

/**
 * HBaseConfig
 * ----------------
 * Author: haqa
 * Date: 06 December 2017 - 16:43
 * Copyright (c) 2017  Office for National Statistics
 */
trait HBaseConfig {

  implicit val configuration: Configuration
  protected val hBaseConfig: Config = configuration.underlying.getConfig("hbase")

  // When running HBase in memory, the namespace has to be ""
  val nameSpace = if (hBaseConfig.getBoolean("in.memory")) "" else hBaseConfig.getString("namespace")

  lazy final val tableName: TableName = TableName.valueOf(
    nameSpace,
    hBaseConfig.getString("table.name"))

  lazy val username: String = hBaseConfig.getString("authentication.username")
  lazy val password: String = hBaseConfig.getString("authentication.password")
  lazy val baseUrl: String = hBaseConfig.getString("rest.endpoint")
  lazy val columnFamily: String = hBaseConfig.getString("column.family")

}
