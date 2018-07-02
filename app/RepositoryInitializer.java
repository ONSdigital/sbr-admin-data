import hbase.connector.HBaseConnector;
import hbase.load.AdminDataLoad;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Configuration;

import javax.inject.Inject;
import java.io.IOException;

/**
 * RepositoryInitializer
 * ----------------
 * Author: haqa
 * Date: 01 February 2018 - 12:45
 * Copyright (c) 2017  Office for National Statistics
 */
class RepositoryInitializer {

    public static final String HEADER_KEY = "csv.header.string";

    private static final String TABLE_NAME_KEY = "hbase.table.name";

    private static final Logger LOG = LoggerFactory.getLogger(RepositoryInitializer.class);

    private final HBaseConnector connector;

    private final Configuration configuration;

    @Inject
    public RepositoryInitializer(HBaseConnector connector, Configuration configuration, AdminDataLoad dataLoader) throws IOException {
        this.connector = connector;
        this.configuration = configuration;

        LOG.info("Started repository initialization. Will create namespace and table if necessary.");
        final String namespace = configuration.getString("hbase.namespace");
        System.setProperty("sbr.hbase.namespace", namespace);
        if (StringUtils.isNotBlank(namespace) && (!namespaceExists(namespace))) createNamespace(namespace);
        final TableName tableName = TableName.valueOf(namespace, configuration.getString(TABLE_NAME_KEY));
        if (!tableExists(tableName)) createTable(tableName);

        LOG.info("Invoking load of admin data.");
        dataLoader.load(configuration.getString(TABLE_NAME_KEY), "201706", configuration.getString("csv.file"), configuration.getInt("csv.id.position"), configuration.getString(HEADER_KEY));
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