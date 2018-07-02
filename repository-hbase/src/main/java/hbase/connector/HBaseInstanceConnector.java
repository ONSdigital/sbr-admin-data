package hbase.connector;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

@Singleton
public class HBaseInstanceConnector extends AbstractHBaseConnector {

    private static final String ZOOKEEPER_QUORUM = "ZOOKEEPER_QUORUM";
    private static final String ZOOKEEPER_PORT = "ZOOKEEPER_PORT";
    private static final String KERBEROS_PRINCIPAL = "KERBEROS_PRINCIPAL";
    private static final String KERBEROS_KEYTAB = "KERBEROS_KEYTAB";
    private static final String KRB5_CONF = "KRB5";
    private static final String HBASE_CONF_DIR = "hbase.conf.dir";
    private static final String HBASE_CONFIGURATION_ZOOKEEPER_QUORUM = "hbase.zookeeper.quorum";
    private static final String HBASE_CONFIGURATION_ZOOKEEPER_CLIENTPORT = "hbase.zookeeper.property.clientPort";
    private static final String JAVA_SECURITY_KRB5_CONF = "java.security.krb5.conf";
    private static final String HBASE_SECURITY_AUTHENTICATION = "hbase.security.authentication";
    private static final String HADOOP_SECURITY_AUTHENTICATION = "hadoop.security.authentication";
    private static final String KERBEROS = "kerberos";

    private static final Logger LOG = LoggerFactory.getLogger(HBaseInstanceConnector.class);

    public HBaseInstanceConnector() throws IOException {
        super();
        setTableName(TableName.valueOf(System.getProperty("hbase.table", "admin_data")));
        connect();
    }

    void connect() throws IOException {
        Configuration configuration;

        // Configure HBase
        String hbaseConfDir = System.getProperty(HBASE_CONF_DIR);
        if (hbaseConfDir == null) {
            configuration = HBaseConfiguration.create();
            LOG.debug("No system property '{}' set so using default configuration", HBASE_CONF_DIR);
        } else {
            if (Files.isDirectory(Paths.get(hbaseConfDir))) {
                File hbaseSiteFile = new File(hbaseConfDir, "/hbase-site.xml");
                if (hbaseSiteFile.exists()) {
                    LOG.debug("Using settings from hbase-site.xml file at '{}'", hbaseSiteFile.getPath());
                    configuration = new Configuration(false);
                    configuration.addResource(hbaseSiteFile.getPath());
                } else {
                    configuration = HBaseConfiguration.create();
                    LOG.warn("No hbase-site.xml file found at '{}' so using default configuration", hbaseSiteFile.getPath());
                }
                File coreSiteFile = new File(hbaseConfDir, "/core-site.xml");
                if (coreSiteFile.exists()) {
                    LOG.debug("Using settings from core-site.xml file at '{}'", coreSiteFile.getPath());
                    configuration.addResource(coreSiteFile.getPath());
                } else {
                    LOG.warn("No core-site.xml file found at '{}' so using default configuration", coreSiteFile.getPath());
                }
            } else {
                configuration = HBaseConfiguration.create();
                LOG.warn("No directory found at '{}' so using default configuration", hbaseConfDir);
            }
        }

        // Authentication required?
        String krb5 = System.getProperty(KRB5_CONF);
        if (krb5 == null) {
            LOG.debug("No system property '{}' set so skipping Kerberos authentication.", KRB5_CONF);
        } else {
            File krb5File = new File(krb5);
            if (krb5File.exists()) {
                LOG.debug("Found krb5.conf file '{}' so performing Kerberos authentication...", krb5File.getPath());

                configuration.set(HBASE_SECURITY_AUTHENTICATION, KERBEROS);
                configuration.set(HADOOP_SECURITY_AUTHENTICATION, KERBEROS);

                String zookeeperQuorum = System.getProperty(ZOOKEEPER_QUORUM);
                String zookeeperPort = System.getProperty(ZOOKEEPER_PORT);

                if (zookeeperQuorum == null) {
                    logSystemPropertyNotFound(ZOOKEEPER_QUORUM);
                } else {
                    logSystemPropertyFound(ZOOKEEPER_QUORUM, zookeeperQuorum);
                    configuration.set(HBASE_CONFIGURATION_ZOOKEEPER_QUORUM, zookeeperQuorum);
                }

                if (zookeeperPort == null) {
                    logSystemPropertyNotFound(ZOOKEEPER_PORT);
                } else {
                    logSystemPropertyFound(ZOOKEEPER_PORT, zookeeperPort);
                    configuration.setInt(HBASE_CONFIGURATION_ZOOKEEPER_CLIENTPORT, Integer.valueOf(zookeeperPort));
                }

                // Point to the krb5.conf file.
                System.setProperty(JAVA_SECURITY_KRB5_CONF, krb5File.getPath());

                // Override these values by setting -DKERBEROS_PRINCIPAL and/or -DKERBEROS_KEYTAB
                String principal = System.getProperty(KERBEROS_PRINCIPAL);
                String keytabLocation = System.getProperty(KERBEROS_KEYTAB);

                if (principal == null) {
                    logSystemPropertyNotFound(KERBEROS_PRINCIPAL);
                } else {
                    logSystemPropertyFound(KERBEROS_PRINCIPAL, principal);
                }

                if (keytabLocation == null) {
                    logSystemPropertyNotFound(KERBEROS_KEYTAB);
                } else {
                    logSystemPropertyFound(KERBEROS_KEYTAB, keytabLocation);
                }

                // Login
                if (principal != null && keytabLocation != null) {
                    UserGroupInformation.setConfiguration(configuration);
                    try {
                        UserGroupInformation ugi = UserGroupInformation.loginUserFromKeytabAndReturnUGI(principal, keytabLocation);
                        LOG.info("Kerberos authentication successful for user '{}' using keytab file '{}'", principal, keytabLocation);
                        LOG.debug("User: '{}'", ugi.getUserName());
                        String[] groups = ugi.getGroupNames();
                        LOG.debug("Groups: ");
                        Arrays.stream(groups).forEach(group -> LOG.debug("'{}' ", group));
                    } catch (IOException e) {
                        LOG.error("Kerberos authentication failed for user '{}' using keytab file '{}'", principal, keytabLocation, e);
                        throw e;
                    }
                }
            } else {
                LOG.warn("No krb5.conf file found at '{}' so skipping Kerberos authentication.", krb5);
            }
        }
        // Initialize connection
        setConfiguration(HBaseConfiguration.create(configuration));
        getConnection();
    }

    private void logSystemPropertyFound(String key, String value) {
        LOG.debug("System property found for '{}' with value '{}'", key, value);
    }

    private void logSystemPropertyNotFound(String key) {
        LOG.warn("No system property found for '{}'", key);
    }

    public static void main(String[] args) throws Exception {
        new HBaseInstanceConnector().validateSchema();
    }

}
