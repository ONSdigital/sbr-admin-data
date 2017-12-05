package hbase.repository;

import akka.actor.ActorSystem;
import hbase.connector.HBaseConnector;
import hbase.util.RowKeyUtils;
import hbase.model.AdminData;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.joda.time.Months;
import org.joda.time.YearMonth;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import scala.Option;
import scala.concurrent.Future;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.apache.hadoop.hbase.util.Bytes.toBytes;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static scala.compat.java8.FutureConverters.toJava;


@RunWith(MockitoJUnitRunner.class)
public class HBaseAdminDataRepositoryTest {

    private static ActorSystem system;
    private static final YearMonth TEST_PERIOD = new YearMonth(2107, 06);

    private HBaseAdminDataRepository repository;
    @Mock
    private HBaseConnector connector;
    @Mock
    private Connection connection;
    @Mock
    private Table table;
    @Mock
    private Result result;

//    @BeforeClass
//    public static void setup() {
//        system = ActorSystem.create(AdminDataExecutionContext.NAME);
//    }

    @Before
    public void setUp() throws Exception {
//        AdminDataExecutionContext context = new AdminDataExecutionContext(system);
        repository = new HBaseAdminDataRepository(connector);
        when(connector.getConnection()).thenReturn(connection);
        when(connection.getTable(any())).thenReturn(table);
        when(table.get(any(Get.class))).thenReturn(result);
    }

    @Test
    public void getCurrentPeriod() throws Exception {
        YearMonth period = toJava(repository.getCurrentPeriod()).toCompletableFuture().get();
        assertEquals("Failure - invalid year", 2017, period.getYear());
        assertEquals("Failure - invalid year", Months.SIX.getMonths(), period.getMonthOfYear());
    }

    @Test
    public void lookupOvertime() throws Exception {
        byte[] columnFamily = toBytes("d");
        // Test data
        String testId = "12335";
        YearMonth testPeriod = new YearMonth(2008, 12);
        // Create cells for each column
        String rowKey = RowKeyUtils.createRowKey(testPeriod, testId);
        Cell nameCell = CellUtil.createCell(Bytes.toBytes(rowKey), columnFamily, Bytes.toBytes("name"), 9223372036854775807L, KeyValue.Type.Maximum, Bytes.toBytes("My Company"), HConstants.EMPTY_BYTE_ARRAY);
        List<Cell> cells = new ArrayList<>();
        cells.add(nameCell);
        when(result.isEmpty()).thenReturn(false);
        when(result.listCells()).thenReturn(cells);
        when(result.getRow()).thenReturn(Bytes.toBytes(rowKey));
        Option<AdminData> result = toJava(repository.lookup(Option.apply(testPeriod), testId)).toCompletableFuture().get();
        assertTrue("Result should be present", result.isDefined());
        assertEquals("Result should be for period 200812", 2008, result.get().referencePeriod().getYear());
        assertEquals("Result should be for period 200812", 12, result.get().referencePeriod().getMonthOfYear());
        assertEquals("Invalid id", "12335", result.get().id());
        assertEquals("Invalid name", "My Company", result.get().variables().get("name").get());

        Option<AdminData> result2 = toJava(repository.lookupOvertime(testId, Option.apply(12L))).toCompletableFuture().get();
    }

    @Test
    public void lookup() throws Exception {
        byte[] columnFamily = toBytes("d");
        // Test data
        String testId = "12335";
        YearMonth testPeriod = new YearMonth(2008, 12);
        // Create cells for each column
        String rowKey = RowKeyUtils.createRowKey(testPeriod, testId);
        Cell nameCell = CellUtil.createCell(Bytes.toBytes(rowKey), columnFamily, Bytes.toBytes("name"), 9223372036854775807L, KeyValue.Type.Maximum, Bytes.toBytes("My Company"), HConstants.EMPTY_BYTE_ARRAY);
        List<Cell> cells = new ArrayList<>();
        cells.add(nameCell);
        when(result.isEmpty()).thenReturn(false);
        when(result.listCells()).thenReturn(cells);
        when(result.getRow()).thenReturn(Bytes.toBytes(rowKey));

        Option<AdminData> result = toJava(repository.lookup(Option.apply(testPeriod), testId)).toCompletableFuture().get();
        assertTrue("Result should be present", result.isDefined());
        assertEquals("Result should be for period 200812", 2008, result.get().referencePeriod().getYear());
        assertEquals("Result should be for period 200812", 12, result.get().referencePeriod().getMonthOfYear());
        assertEquals("Invalid id", "12335", result.get().id());
        assertEquals("Invalid name", "My Company", result.get().variables().get("name").get());
    }

    @Test
    public void lookupNoDataFound() throws Exception {
        when(result.isEmpty()).thenReturn(true);
        assertEquals("Result should be empty", Option.empty(), toJava(repository.lookup(Option.apply(TEST_PERIOD), "12345")).toCompletableFuture().get());
    }

    @Test(expected = Exception.class)
    public void lookupException() throws Exception {
        when(connection.getTable(any())).thenThrow(new IOException("Failed to retrieve data"));
        Future<Option<AdminData>> stage = repository.lookup(Option.apply(TEST_PERIOD), "12345");
        toJava(stage).toCompletableFuture().get();
    }

//    @AfterClass
//    public static void teardown() {
//        TestKit.shutdownActorSystem(system);
//        system = null;
//    }

}