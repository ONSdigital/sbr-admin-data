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
  private val hBaseConfig: Config = configuration.underlying.getConfig("hbase")

  private val nameSpace: String = if (hBaseConfig.getBoolean("in.memory.init")) {
    hBaseConfig.getString("in.memory.namespace")
  } else { hBaseConfig.getString("rest.namespace") }

  lazy val tableName: TableName = TableName.valueOf(
    nameSpace,
    hBaseConfig.getString("table.name"))

  lazy val username: String = hBaseConfig.getString("authentication.username")
  lazy val password: String = hBaseConfig.getString("authentication.password")
  lazy val baseUrl: String = hBaseConfig.getString("rest.endpoint")
  lazy val columnFamily: String = hBaseConfig.getString("column.family")

}
