import java.util.concurrent.TimeUnit
import javax.inject.{ Inject, Provider }

import com.codahale.metrics.{ ConsoleReporter, MetricRegistry, Slf4jReporter }
import com.google.inject.AbstractModule
import hbase.connector.{ HBaseConnector, HBaseInMemoryConnector }
import hbase.load.{ AdminDataLoad, CSVDataKVMapper, HBaseAdminDataLoader }
import hbase.repository.{ AdminDataRepository, InMemoryAdminDataRepository, RestAdminDataRepository }
import org.slf4j.LoggerFactory
import play.{ Configuration, Environment }

/**
  * This class is a Guice module that tells Guice how to bind several
  * different types. This Guice module is created when the Play
  * application starts.
  * <p>
  * Play will automatically use any class called `Module` that is in
  * the root package. You can create modules in other locations by
  * adding `play.modules.enabled` settings to the `application.conf`
  * configuration file.
  */
class Module(val environment: Environment, val configuration: Configuration) extends AbstractModule {
  override def configure(): Unit = {
    if (configuration.getBoolean("database.in.memory")) {
      System.setProperty(CSVDataKVMapper.HEADER_STRING, configuration.getString("csv.header.string"))
      bind(classOf[HBaseConnector]).toInstance(new HBaseInMemoryConnector(configuration.getString("database.table")))
      bind(classOf[RepositoryInitializer]).asEagerSingleton()
      bind(classOf[AdminDataRepository]).to(classOf[InMemoryAdminDataRepository]).asEagerSingleton()
      bind(classOf[AdminDataLoad]).to(classOf[HBaseAdminDataLoader]).asEagerSingleton()
    } else bind(classOf[AdminDataRepository]).to(classOf[RestAdminDataRepository]).asEagerSingleton()
    if (configuration.getBoolean("api.metrics"))
      bind(classOf[MetricRegistry])
        .toProvider(classOf[MetricRegistryProvider])
        .asEagerSingleton()
  }
}

class RepositoryInitializer {
  @Inject
  @throws[Exception]
  def RepositoryInitializer(configuration: Configuration, dataLoader: AdminDataLoad) {
    dataLoader.load(configuration.getString("database.table"), "201706", configuration.getString("csv.file"))
  }
}

object MetricRegistryProvider {
  private val logger = LoggerFactory.getLogger("application.Metrics")
  private val registry = new MetricRegistry
}

class MetricRegistryProvider() extends Provider[MetricRegistry] {
  consoleReporter()
  slf4jReporter()

  private def consoleReporter(): Unit = {
    val reporter = ConsoleReporter
      .forRegistry(MetricRegistryProvider.registry)
      .convertRatesTo(TimeUnit.SECONDS)
      .convertDurationsTo(TimeUnit.MILLISECONDS)
      .build
    reporter.start(1, TimeUnit.SECONDS)
  }

  private def slf4jReporter(): Unit = {
    val reporter = Slf4jReporter
      .forRegistry(MetricRegistryProvider.registry)
      .outputTo(MetricRegistryProvider.logger)
      .convertRatesTo(TimeUnit.SECONDS)
      .convertDurationsTo(TimeUnit.MILLISECONDS)
      .build
    reporter.start(1, TimeUnit.MINUTES)
  }

  override def get: MetricRegistry = MetricRegistryProvider.registry
}
