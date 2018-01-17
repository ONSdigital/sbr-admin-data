package hbase.load;

import com.typesafe.config.Config;
import hbase.connector.HBaseConnector;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.api.Configuration;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Collections;

import static org.apache.hadoop.util.ToolRunner.run;

@Singleton
public class HBaseAdminDataLoader implements AdminDataLoad {

    private static final Logger LOG = LoggerFactory.getLogger(HBaseAdminDataLoader.class);

    @Inject
    private HBaseConnector connector;

    @Inject
    private BulkLoader bulkLoader;

    @Inject
    private Configuration configuration;


    @Override
    public int load(String tableName, String referencePeriod, String inputFile) {
        final String namespace = configuration.underlying().getString("hbase.namespace");
        try {
            if (!namespaceExists(namespace)) createNamespace(namespace);
            if (!tableExists(tableName)) createTable(tableName);
            return run(connector.getConfiguration(), bulkLoader, new String[]{tableName, referencePeriod, inputFile});
        } catch (Exception e) {
            LOG.error("Load failed", e);
            return -1;
        }
    }


    private void createTable(String tableName) throws IOException {
        try(Connection connection = connector.getConnection()) {
            HTableDescriptor htable = new HTableDescriptor(
                TableName.valueOf(configuration.underlying().getString("hbase.namespace"), tableName)
            );
            htable.addFamily( new HColumnDescriptor(configuration.underlying().getString("hbase.column.family")));
            connection.getAdmin().createTable(htable);
        }
    }

    private boolean tableExists(String tableName) throws IOException {
        try(Connection connection = connector.getConnection()) {
            return connection.getAdmin().tableExists(TableName.valueOf(tableName));
        }
    }

    private void createNamespace(String namespace) throws IOException {
        try(Connection connection = connector.getConnection()) {
            connection.getAdmin().createNamespace(NamespaceDescriptor.create(namespace).build());
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
