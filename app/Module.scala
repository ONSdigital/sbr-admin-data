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
    if (configuration.getBoolean("hbase.in.memory.init")) {
      System.setProperty(CSVDataKVMapper.HEADER_STRING, configuration.getString("csv.header.string"))
      bind(classOf[HBaseConnector]).toInstance(new HBaseInMemoryConnector(configuration.getString("hbase.table.name")))
      bind(classOf[RepositoryInitializer]).asEagerSingleton()
      bind(classOf[AdminDataRepository]).to(classOf[InMemoryAdminDataRepository]).asEagerSingleton()
      bind(classOf[AdminDataLoad]).to(classOf[HBaseAdminDataLoader]).asEagerSingleton()
    } else { bind(classOf[AdminDataRepository]).to(classOf[RestAdminDataRepository]).asEagerSingleton() }
    if (configuration.getBoolean("api.metrics")) {
      bind(classOf[MetricRegistry])
        .toProvider(classOf[MetricRegistryProvider])
        .asEagerSingleton()
    }
  }
}

class RepositoryInitializer {
  @Inject
  @throws[Exception]
  def RepositoryInitializer(configuration: Configuration, dataLoader: AdminDataLoad) {
    dataLoader.load(configuration.getString("hbase.table.name"), "201706", configuration.getString("csv.file"))
  }
}

class MetricRegistryProvider() extends Provider[MetricRegistry] {
  consoleReporter()
  slf4jReporter()

  private def consoleReporter(): Unit = {
    val reporter = ConsoleReporter
      .forRegistry(MetricRegistryProvider.REGISTRY)
      .convertRatesTo(TimeUnit.SECONDS)
      .convertDurationsTo(TimeUnit.MILLISECONDS)
      .build
    reporter.start(1, TimeUnit.SECONDS)
  }

  private def slf4jReporter(): Unit = {
    val reporter = Slf4jReporter
      .forRegistry(MetricRegistryProvider.REGISTRY)
      .outputTo(MetricRegistryProvider.LOGGER)
      .convertRatesTo(TimeUnit.SECONDS)
      .convertDurationsTo(TimeUnit.MILLISECONDS)
      .build
    reporter.start(1, TimeUnit.MINUTES)
  }

  override def get: MetricRegistry = MetricRegistryProvider.REGISTRY
}

object MetricRegistryProvider {
  private val LOGGER = LoggerFactory.getLogger("application.Metrics")
  private val REGISTRY = new MetricRegistry
}
