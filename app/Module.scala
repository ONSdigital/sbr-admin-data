import java.util.concurrent.TimeUnit
import javax.inject.Provider

import play.{Configuration, Environment}
import org.slf4j.LoggerFactory
import com.codahale.metrics.{ConsoleReporter, MetricRegistry, Slf4jReporter}
import com.google.inject.AbstractModule

import hbase.connector.{HBaseConnector, HBaseInMemoryConnector}
import hbase.load.{AdminDataLoad, BulkLoader, HBaseAdminDataLoader}
import hbase.repository.{AdminDataRepository, InMemoryAdminDataRepository, RestAdminDataRepository}

/**
  * Module
  * ----------------
  * Date: 01 February 2018 - 12:22
  * Copyright (c) 2017  Office for National Statistics
  */
class Module(val environment: Environment, val configuration: Configuration) extends AbstractModule {
  override def configure(): Unit = {
    if (configuration.getBoolean("hbase.initialize")) {
      System.setProperty(BulkLoader.HEADER_STRING, configuration.getString("csv.header.string"))
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