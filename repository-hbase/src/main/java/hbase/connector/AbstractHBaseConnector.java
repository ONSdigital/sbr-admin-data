package hbase.connector;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public abstract class AbstractHBaseConnector implements HBaseConnector {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractHBaseConnector.class.getName());
    static final byte[] COLUMN_FAMILY = Bytes.toBytes("d");
    private Configuration configuration;
    private Connection connection;

    private TableName tableName;

    AbstractHBaseConnector() {
    }

    void setConfiguration(Configuration configuration)  {
        this.configuration = configuration;
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    public Connection getConnection() throws IOException {
        if (connection == null || connection.isClosed() || connection.isAborted()) {
            try {
                connection = ConnectionFactory.createConnection(configuration);
            } catch (IOException e) {
                LOG.error("Error getting connection to HBase", e);
                throw e;
            }
        }
        return connection;
    }

    public void setTableName(TableName tableName) {
        this.tableName = tableName;
    }

    TableName getTableName() {
        return tableName;
    }

    void validateSchema() throws Exception {
        LOG.info("Validating schema...");
        boolean isValid = true;
        try (Admin hbaseAdmin = getConnection().getAdmin()) {
            if (hbaseAdmin.tableExists(tableName)) {
                LOG.info("{}' table exists", tableName.getNameAsString());
            } else {
                LOG.error("{}' table does not exist!", tableName.getNameAsString());
                isValid = false;
            }
        }
        if (isValid) {
            LOG.info("Valid schema!");
        } else {
            LOG.error("Invalid schema!");
        }
    }

    @Override
    protected void finalize() throws Throwable {
        if (connection != null) connection.close();
    }
}
