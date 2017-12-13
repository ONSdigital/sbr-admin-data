package config

import com.typesafe.config.{ Config, ConfigFactory }

import scala.concurrent.duration._

/**
 * Created by coolit on 23/11/2017.
 */
object Properties {

  private val config: Config = SBRPropertiesConfiguration.envConfig(ConfigFactory.load())

  // CircuitBreaker
  lazy val cbMaxFailures: Int = config.getInt("circuit_breaker.maxFailures")
  lazy val cbCallTimeout: FiniteDuration = config.getInt("circuit_breaker.callTimeout") seconds
  lazy val cbResetTimeout: FiniteDuration = config.getInt("circuit_breaker.resetTimeout") seconds

  // Caching
  lazy val cacheDelimiter: String = config.getString("cache.admin_data.delimiter")
  lazy val cacheDuration: FiniteDuration = config.getInt("cache.admin_data.duration") minutes
}
