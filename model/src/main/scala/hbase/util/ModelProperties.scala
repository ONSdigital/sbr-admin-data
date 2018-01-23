package hbase.util

import play.api.Configuration
import com.typesafe.config.Config

/**
 * Properties
 * ----------------
 * Author: haqa
 * Date: 22 January 2018 - 16:19
 * Copyright (c) 2017  Office for National Statistics
 */
trait ModelProperties {
  implicit val configuration: Configuration
  lazy val configModel: Config = configuration.underlying

  lazy val MAX_RESULT_SIZE: Long = configModel.getLong("hbase.max.response.length")

}
