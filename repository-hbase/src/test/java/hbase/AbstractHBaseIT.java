package hbase;

import hbase.connector.HBaseConnector;
import hbase.connector.HBaseInMemoryConnector;
import org.junit.BeforeClass;

public abstract class AbstractHBaseIT {

    protected static HBaseConnector HBASE_CONNECTOR;
    protected static String TABLE_NAME = "test_table";

    @BeforeClass
    public static void init() throws Exception {
        HBASE_CONNECTOR = new HBaseInMemoryConnector(TABLE_NAME);
        HBASE_CONNECTOR.getConnection();
    }

}
