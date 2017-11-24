package config

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

/**
 * Created by coolit on 23/11/2017.
 */
object SBRPropertiesConfiguration extends LazyLogging {

  def envConfig(conf: Config): Config = {
    val env = sys.props.get("environment").getOrElse("default")
    logger.info(s"Load config for [$env] env")
    //val envConf = conf.getConfig(s"env.$env")
    //logger.debug(envConf.toString)
    //envConf
    conf
  }
}