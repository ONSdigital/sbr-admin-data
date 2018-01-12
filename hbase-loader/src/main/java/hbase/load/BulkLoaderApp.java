package hbase.load;

import hbase.connector.HBaseConnector;
import hbase.connector.HBaseInMemoryConnector;
import org.apache.hadoop.util.ToolRunner;

import static hbase.load.ExitSignals.*;

public class BulkLoaderApp {

    public static void main(String[] args) {
        try {
            HBaseConnector connector = new HBaseInMemoryConnector(args[1]);
            int result = ToolRunner.run(connector.getConfiguration(), new BulkLoader(connector), args);
            System.exit(result);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(ERROR);
        }
    }

}
