
import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.google.inject.AbstractModule;
import hbase.connector.HBaseConnector;
import hbase.connector.HBaseInMemoryConnector;
import hbase.connector.HBaseInstanceConnector;
import hbase.load.CSVDataKVMapper;
import hbase.load.HBaseAdminDataLoader;
import hbase.repository.HBaseAdminDataRepository;
import hbase.load.AdminDataLoad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Configuration;
import play.Environment;
import hbase.repository.AdminDataRepository;

import javax.inject.Inject;
import javax.inject.Provider;
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
        if (configuration.getBoolean("database.in.memory")) {
            System.setProperty(CSVDataKVMapper.HEADER_STRING, configuration.getString("csv.header.string"));
            bind(HBaseConnector.class).toInstance(new HBaseInMemoryConnector(configuration.getString("database.table")));
            bind(RepositoryInitializer.class).asEagerSingleton();
        } else {
            bind(HBaseConnector.class).to(HBaseInstanceConnector.class).asEagerSingleton();
        }
        bind(AdminDataRepository.class).to(HBaseAdminDataRepository.class).asEagerSingleton();
        bind(AdminDataLoad.class).to(HBaseAdminDataLoader.class).asEagerSingleton();
        if (configuration.getBoolean("api.metrics")) {
            bind(MetricRegistry.class).toProvider(MetricRegistryProvider.class).asEagerSingleton();
        }
    }
}

class RepositoryInitializer {

    @Inject
    public RepositoryInitializer(Configuration configuration, AdminDataLoad dataLoader) throws Exception {
        dataLoader.load(configuration.getString("database.table"), "201706", configuration.getString("csv.file"));
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
