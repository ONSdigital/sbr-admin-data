package config

import scala.concurrent.duration._

import play.api.Configuration
import com.typesafe.config.Config

// @TODO - does this need to be a trait => object
trait Properties {

  implicit val configuration: Configuration
  lazy private val config: Config = configuration.underlying

  // CircuitBreaker
  lazy val cbMaxFailures: Int = config.getInt("circuit.breaker.max.failures")
  lazy val cbCallTimeout: FiniteDuration = config.getInt("circuit.breaker.call.timeout") seconds
  lazy val cbResetTimeout: FiniteDuration = config.getInt("circuit.breaker.reset.timeout") seconds

  // Caching
  lazy val cacheDelimiter: String = config.getString("cache.delimiter")
  lazy val cacheDuration: FiniteDuration = config.getInt("cache.duration") minutes

  // Validation
  lazy val idRegex: String = config.getString("validation.id.regex")

}
