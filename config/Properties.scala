package config

import com.typesafe.config.{ Config, ConfigFactory }

/**
  * Properties
  * ----------------
  * Author: haqa
  * Date: 06 November 2017 - 10:06
  * Copyright (c) 2017  Office for National Statistics
  */
object Properties {

  // SBRPropertiesConfiguration.envConfig(ConfigFactory.load())
  private val config: Config = ConfigFactory.load()

  lazy val dbConfig = config.getConfig("circuit-breaker")

  //circuit-breaker vars
  lazy val circuitBreakerFailureThreshold: Int = config.getInt("failure.threshold")
  lazy val circuitBreakerFailureDeclarationTime: Int = config.getInt("failure.declaration.time")
  lazy val circuitBreakerResetTime: Int = config.getInt("reset.timeout")

  // db
//  lazy val defaultDBInit: String = config.getString("db.default.name")


}