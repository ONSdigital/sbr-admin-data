package hbase.connector;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.Connection;

import java.io.IOException;

public interface HBaseConnector {

    Configuration getConfiguration();

    Connection getConnection() throws IOException;
}
