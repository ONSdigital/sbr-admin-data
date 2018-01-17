package hbase.load;

import hbase.connector.HBaseConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

import static org.apache.hadoop.util.ToolRunner.run;

@Singleton
public class HBaseAdminDataLoader implements AdminDataLoad {

    private static final Logger LOG = LoggerFactory.getLogger(HBaseAdminDataLoader.class);

    @Inject
    private HBaseConnector connector;

    @Inject
    private BulkLoader bulkLoader;


    @Override
    public int load(String tableName, String referencePeriod, String inputFile) {
        try {
            return run(connector.getConfiguration(), bulkLoader, new String[]{tableName, referencePeriod, inputFile});
        } catch (Exception e) {
            LOG.error("Load failed", e);
            return -1;
        }
    }

}
