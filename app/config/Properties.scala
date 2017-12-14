package config

import scala.concurrent.duration._

import play.api.Configuration
import com.typesafe.config.Config

trait Properties {

  implicit val configuration: Configuration
  lazy val config: Config = configuration.underlying

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
