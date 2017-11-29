package config

import com.typesafe.config.{ Config, ConfigFactory }

import scala.concurrent.duration._

/**
 * Created by coolit on 23/11/2017.
 */
object Properties {

  private val config: Config = SBRPropertiesConfiguration.envConfig(ConfigFactory.load())

  // CircuitBreaker
  lazy val cbMaxFailures = config.getInt("circuit_breaker.maxFailures")
  lazy val cbCallTimeout = config.getInt("circuit_breaker.callTimeout") seconds
  lazy val cbResetTimeout = config.getInt("circuit_breaker.resetTimeout") minute

  // Caching
  lazy val cacheDelimiter = config.getString("cache.admin_data.delimiter")
  lazy val cacheDuration = config.getInt("cache.admin_data.duration") minutes
}
