package hbase.connector;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.TableName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.IOException;

@Singleton
public class HBaseInMemoryConnector extends AbstractHBaseConnector {

    private static final Logger LOG = LoggerFactory.getLogger(HBaseInMemoryConnector.class);
    private static HBaseTestingUtility hBaseTestingUtility;

    public HBaseInMemoryConnector(String tableName)  {
        LOG.info("Using in memory HBase database");
        // In memory database does not support namespaces so set to empty string
        System.setProperty("hbase.namespace", "");
        System.setProperty("hbase.table", tableName);
        setTableName(TableName.valueOf(tableName));
        setConfiguration(init());
    }

    private Configuration init() {
        if (hBaseTestingUtility == null) {
            LOG.info("Starting in memory HBase instance...");
            hBaseTestingUtility = new HBaseTestingUtility();
            try {
                hBaseTestingUtility.setJobWithoutMRCluster();
                hBaseTestingUtility.startMiniCluster();
                hBaseTestingUtility.createTable(getTableName(), COLUMN_FAMILY);
            } catch (Exception e) {
                e.printStackTrace();
                LOG.info("In memory HBase instance failed to start");
            }
            LOG.info("In memory HBase instance started ({})", getTableName());
        }
        return hBaseTestingUtility.getConfiguration();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        LOG.info("Stopping in memory Hbase instance...");
        hBaseTestingUtility.cleanupDataTestDirOnTestFS(getTableName().getNameAsString());
        hBaseTestingUtility.shutdownMiniCluster();
        LOG.info("In memory Hbase instance shutdown");
    }

}
