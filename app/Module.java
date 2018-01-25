import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.google.inject.AbstractModule;
import hbase.connector.HBaseConnector;
import hbase.connector.HBaseInstanceConnector;
import hbase.load.AdminDataLoad;
import hbase.load.HBaseAdminDataLoader;
import hbase.repository.AdminDataRepository;
import hbase.repository.HBaseAdminDataRepository;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Configuration;
import play.Environment;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.IOException;
import java.util.concurrent.TimeUnit;


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
public class Module extends AbstractModule {

    private final Configuration configuration;

    public Module(Environment environment, Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void configure() {
        if (configuration.getBoolean("hbase.initialize")) {
            System.setProperty(RepositoryInitializer.HEADER_KEY, configuration.getString(RepositoryInitializer.HEADER_KEY));
            bind(RepositoryInitializer.class).asEagerSingleton();
        }
        bind(HBaseConnector.class).to(HBaseInstanceConnector.class);
        bind(AdminDataRepository.class).to(HBaseAdminDataRepository.class);
        bind(AdminDataLoad.class).to(HBaseAdminDataLoader.class);
        if (configuration.getBoolean("api.metrics")) {
            bind(MetricRegistry.class).toProvider(MetricRegistryProvider.class);
        }
    }
}

class RepositoryInitializer {

    public static final String HEADER_KEY = "csv.header.string";

    private static final Logger LOG = LoggerFactory.getLogger(RepositoryInitializer.class);

    private final HBaseConnector connector;

    private final Configuration configuration;

    @Inject
    public RepositoryInitializer(HBaseConnector connector, Configuration configuration, AdminDataLoad dataLoader) throws IOException {
        this.connector = connector;
        this.configuration = configuration;

        LOG.info("Started repository initialization. Will create namespace and table if necessary.");
        final String namespace = configuration.getString("hbase.namespace");
        if (StringUtils.isNotBlank(namespace) && (!namespaceExists(namespace))) createNamespace(namespace);
        final TableName tableName = TableName.valueOf(namespace, configuration.getString("hbase.table.name"));
        if (!tableExists(tableName)) createTable(tableName);

        LOG.info("Invoking load of admin data.");
        dataLoader.load(tableName.getNameWithNamespaceInclAsString(), "201706", configuration.getString("csv.file"), configuration.getInt("csv.id.position"), configuration.getString(HEADER_KEY));
    }

    private void createTable(TableName tableName) throws IOException {
        try(Connection connection = connector.getConnection()) {
            HTableDescriptor htable = new HTableDescriptor(tableName);
            htable.addFamily( new HColumnDescriptor(configuration.getString("hbase.column.family")));
            connection.getAdmin().createTable(htable);
            LOG.info("Created table {}", tableName);

        } catch (TableExistsException e) {
            LOG.warn("Failed to create table {} since it already exists", tableName);
            LOG.debug("Failed to create table " + tableName, e);

        } catch (IOException e) {
            LOG.error("Failed to create table " + tableName, e);
            throw e;
        }
    }

    private boolean tableExists(TableName tableName) throws IOException {
        try(Connection connection = connector.getConnection()) {
            return connection.getAdmin().tableExists(tableName);
        }
    }

    private void createNamespace(String namespace) throws IOException {
        try(Connection connection = connector.getConnection()) {
            connection.getAdmin().createNamespace(NamespaceDescriptor.create(namespace).build());
            LOG.info("Created namespace {}", namespace);

        } catch (NamespaceExistException e) {
            LOG.warn("Failed to create namespace {} since it already exists", namespace);
            LOG.debug("Failed to create namespace " + namespace, e);

        } catch (IOException e) {
            LOG.error("Failed to create namespace " + namespace, e);
            throw e;
        }
    }

    private boolean namespaceExists(String namespace) throws IOException {
        try(Connection connection = connector.getConnection()) {
            connection.getAdmin().getNamespaceDescriptor(namespace);
            return true;

        } catch (NamespaceNotFoundException e) {
            return false;
        }
    }
}


class MetricRegistryProvider implements Provider<MetricRegistry> {
    private static final Logger logger = LoggerFactory.getLogger("application.Metrics");

    private static final MetricRegistry registry = new MetricRegistry();

    private void consoleReporter() {
        ConsoleReporter reporter = ConsoleReporter.forRegistry(registry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.start(1, TimeUnit.SECONDS);
    }

    private void slf4jReporter() {
        final Slf4jReporter reporter = Slf4jReporter.forRegistry(registry)
                .outputTo(logger)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.start(1, TimeUnit.MINUTES);
    }

    public MetricRegistryProvider() {
        consoleReporter();
        slf4jReporter();
    }

    @Override
    public MetricRegistry get() {
        return registry;
    }
}
