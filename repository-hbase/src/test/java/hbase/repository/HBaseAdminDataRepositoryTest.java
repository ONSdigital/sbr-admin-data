package hbase.repository;

import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import hbase.connector.HBaseConnector;
import hbase.util.RowKeyUtils;
import model.AdminData;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.time.Month;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import static org.apache.hadoop.hbase.util.Bytes.toBytes;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HBaseAdminDataRepositoryTest {

    private static ActorSystem system;
    private HBaseAdminDataRepository repository;
    @Mock
    private HBaseConnector connector;
    @Mock
    private Connection connection;
    @Mock
    private Table table;
    @Mock
    private Result result;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create(AdminDataExecutionContext.NAME);
    }

    @Before
    public void setUp() throws Exception {
        AdminDataExecutionContext context = new AdminDataExecutionContext(system);
        repository = new HBaseAdminDataRepository(connector, context);
        when(connector.getConnection()).thenReturn(connection);
        when(connection.getTable(any())).thenReturn(table);
        when(table.get(any(Get.class))).thenReturn(result);
    }

    @Test
    public void getCurrentPeriod() throws Exception {
        YearMonth period = repository.getCurrentPeriod().toCompletableFuture().get();
        assertEquals("Failure - invalid year", 2017, period.getYear());
        assertEquals("Failure - invalid year", Month.JUNE, period.getMonth());
    }

    @Test
    public void lookup() throws Exception {
        byte[] columnFamily = toBytes("d");
        // Test data
        String testId = "12335";
        YearMonth testPeriod = YearMonth.of(2008, 12);
        // Create cells for each column
        String rowKey = RowKeyUtils.createRowKey(testPeriod, testId);
        Cell nameCell = CellUtil.createCell(Bytes.toBytes(rowKey), columnFamily, Bytes.toBytes("name"), 9223372036854775807L, KeyValue.Type.Maximum, Bytes.toBytes("My Company"), HConstants.EMPTY_BYTE_ARRAY);
        List<Cell> cells = new ArrayList<>();
        cells.add(nameCell);
        when(result.isEmpty()).thenReturn(false);
        when(result.listCells()).thenReturn(cells);
        when(result.getRow()).thenReturn(Bytes.toBytes(rowKey));

        Optional<AdminData> result = repository.lookup(testPeriod, testId).toCompletableFuture().get();
        assertTrue("Result should be present", result.isPresent());
        assertEquals("Result should be for period 200812", 2008, result.get().getReferencePeriod().getYear());
        assertEquals("Result should be for period 200812", Month.DECEMBER, result.get().getReferencePeriod().getMonth());
        assertEquals("Invalid id", "12335", result.get().getId());
        assertEquals("Invalid name", "My Company", result.get().getVariables().get("name"));
    }

    @Test
    public void lookupNoDataFound() throws Exception {
        when(result.isEmpty()).thenReturn(true);
        assertEquals("Result should be empty", Optional.empty(), repository.lookup(YearMonth.of(2107, 06), "12345").toCompletableFuture().get());
    }

    @Test(expected = ExecutionException.class)
    public void lookupException() throws Exception {
        when(connection.getTable(any())).thenThrow(new IOException("Failed to retrieve data"));
        CompletionStage<Optional<AdminData>> stage = repository.lookup(YearMonth.of(2107, 06), "12345");
        stage.toCompletableFuture().get();
    }

    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

}