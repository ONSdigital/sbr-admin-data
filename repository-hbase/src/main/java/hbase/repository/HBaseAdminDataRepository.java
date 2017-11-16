package hbase.repository;

import hbase.connector.HBaseConnector;
import hbase.util.RowKeyUtils;
import model.AdminData;
import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.Failsafe;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.AdminDataRepository;

import javax.inject.Inject;
import java.time.Month;
import java.time.YearMonth;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * HBase implementation of an Admin Data DAO
 */
public class HBaseAdminDataRepository implements AdminDataRepository {

    private static final Logger LOG = LoggerFactory.getLogger(HBaseAdminDataRepository.class.getName());
    private static final YearMonth HARDCODED_CURRENT_PERIOD = YearMonth.of(2017, Month.JUNE);
    private final CircuitBreaker circuitBreaker = new CircuitBreaker().withDelay(1, TimeUnit.MINUTES).withFailureThreshold(1).withSuccessThreshold(3);

    private final Executor ec;
    private final HBaseConnector connector;
    private final TableName tableName;

    @Inject
    public HBaseAdminDataRepository(HBaseConnector connector, Executor ec) {
        this.connector = connector;
        this.ec = ec;
        this.tableName = TableName.valueOf(System.getProperty("hbase.namespace", ""), System.getProperty("hbase.table", "data"));
    }

    @Override
    public CompletionStage<YearMonth> getCurrentPeriod() {
        //TODO: Implement retrieving the current period from HBase
        return supplyAsync(() -> Failsafe.with(circuitBreaker).get(() -> HARDCODED_CURRENT_PERIOD), ec);
    }

    @Override
    public CompletionStage<Optional<AdminData>> lookup(YearMonth referencePeriod, String key) {
        return supplyAsync(() -> Failsafe.with(circuitBreaker).get(() -> getAdminData(referencePeriod, key)), ec);
    }

    private Optional<AdminData> getAdminData(YearMonth referencePeriod, String key) throws Exception {
        String rowKey = RowKeyUtils.createRowKey(referencePeriod, key);
        Optional<AdminData> adminData;
        try (Table table = connector.getConnection().getTable(tableName)) {
            Get get = new Get(Bytes.toBytes(rowKey));
            Result result = table.get(get);
            if (result.isEmpty()) {
                LOG.debug("No data found for row key '{}'", rowKey);
                adminData = Optional.empty();
            } else {
                LOG.debug("Found data for row key '{}'", rowKey);
                adminData = Optional.of(convertToAdminData(result));
            }
        } catch (Exception e) {
            LOG.error("Error getting data for row key '{}'", rowKey, e);
            throw e;
        }
        return adminData;
    }

    private AdminData convertToAdminData(Result result) {
        AdminData adminData = RowKeyUtils.createAdminDataFromRowKey(Bytes.toString(result.getRow()));
        for (Cell cell : result.listCells()) {
            String column = new String(CellUtil.cloneQualifier(cell));
            String value = new String(CellUtil.cloneValue(cell));
            LOG.debug("Found data column '{}' with value '{}'", column, value);
            adminData.putVariable(column, value);
        }
        return adminData;
    }

}

