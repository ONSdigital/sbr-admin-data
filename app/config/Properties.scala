package config

import com.typesafe.config.{ Config, ConfigFactory }

import scala.concurrent.duration._

/**
 * Created by coolit on 23/11/2017.
 */
object Properties {

  private val config: Config = SBRPropertiesConfiguration.envConfig(ConfigFactory.load())

  // CircuitBreaker
  lazy val cbMaxFailures = config.getInt("circuitBreaker.maxFailures")
  lazy val cbCallTimeout = config.getInt("circuitBreaker.callTimeout") seconds
  lazy val cbResetTimeout = config.getInt("circuitBreaker.resetTimeout") seconds

  // Caching
  lazy val cacheDelimiter = config.getString("cache.delimiter")
  lazy val cacheDuration = config.getInt("cache.duration") minutes

  // Validation
  lazy val idRegex = config.getString("validation.idRegex")
}
