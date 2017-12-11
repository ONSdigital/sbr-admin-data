package hbase.util

import org.apache.hadoop.hbase.TableName
import com.typesafe.config.{ Config, ConfigFactory }

/**
 * HBaseConfig
 * ----------------
 * Author: haqa
 * Date: 06 December 2017 - 16:43
 * Copyright (c) 2017  Office for National Statistics
 */
object HBaseConfig {
  lazy val config: Config = ConfigFactory.load().getConfig("hbase")

  lazy final val tableName: TableName = TableName.valueOf(
    config.getString("namespace"),
    config.getString("table.name"))
  lazy val username: String = config.getString("authentication.username")
  lazy val password: String = config.getString("authentication.password")
  lazy val baseUrl: String = config.getString("rest.endpoint")
  lazy val columnFamily: String = config.getString("column.family")

}
