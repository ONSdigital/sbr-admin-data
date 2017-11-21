package hbase.load;

import akka.actor.ActorSystem;
import hbase.AbstractHBaseIT;
import hbase.util.RowKeyUtils;
import model.AdminData;
import org.apache.hadoop.util.ToolRunner;
import org.joda.time.YearMonth;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import hbase.repository.HBaseAdminDataRepository;
import scala.Option;
import scala.concurrent.Future;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static scala.compat.java8.FutureConverters.toJava;

public class BulkLoaderTest extends AbstractHBaseIT {

    private static final String TEST_CSV_ID_COLUMN = "1";
    private static final YearMonth TEST_PERIOD = new YearMonth(2017, 06);
    private static final String TEST_PERIOD_STR = TEST_PERIOD.toString(RowKeyUtils.REFERENCE_PERIOD_FORMAT());
    private static final String TEST_CH_CSV = "test/resources/ch-data.csv";
    private static final String TEST_PAYE_CSV = "test/resources/paye-data.csv";
    private static final String TEST_VAT_CSV = "test/resources/vat-data.csv";
    private BulkLoader bulkLoader;
    private static ActorSystem system;
    private HBaseAdminDataRepository repository;


    @BeforeClass
    public static void setup() {
//        system = ActorSystem.create(AdminDataExecutionContext.NAME);
        System.setProperty(CSVDataKVMapper.ROWKEY_POSITION, TEST_CSV_ID_COLUMN);
    }


    @Before
    public void setUp() throws Exception {
//        AdminDataExecutionContext context = new AdminDataExecutionContext(system);
        repository = new HBaseAdminDataRepository(HBASE_CONNECTOR);
        bulkLoader = new BulkLoader(HBASE_CONNECTOR);
    }

    @Test
    public void loadCompaniesData() throws Exception {
        System.setProperty(CSVDataKVMapper.HEADER_STRING, "companyname");
        File file = new File(TEST_CH_CSV);
        assertTrue("Test file not found " + file.getCanonicalPath(), file.exists());

        int result = loadData(new String[]{TABLE_NAME, TEST_PERIOD_STR, TEST_CH_CSV});
        assertEquals("Bulk load failed", 0, result);

        Future<Option<AdminData>> company = repository.lookup(TEST_PERIOD, "04375380");
        assertTrue("No company registration found", toJava(company).toCompletableFuture().get().isEmpty());

        assertEquals("No company registration found", "04375380", toJava(company).toCompletableFuture().get().get().id());
    }

    @Test
    public void loadPAYEData() throws Exception {
        System.setProperty(CSVDataKVMapper.HEADER_STRING, "entref");
        File file = new File(TEST_PAYE_CSV);
        assertTrue("Test file not found " + file.getCanonicalPath(), file.exists());

        int result = loadData(new String[]{TABLE_NAME, TEST_PERIOD_STR, TEST_PAYE_CSV});
        assertEquals("Bulk load failed", 0, result);

        Future<Option<AdminData>> payeReturn = repository.lookup(TEST_PERIOD, "8878574");
        assertTrue("No PAYE record found", toJava(payeReturn).toCompletableFuture().get().isEmpty());

        assertEquals("No PAYE record found", "8878574", toJava(payeReturn).toCompletableFuture().get().get().id());
    }

    @Test
    public void loadVATData() throws Exception {
        System.setProperty(CSVDataKVMapper.HEADER_STRING, "entref");
        File file = new File(TEST_VAT_CSV);
        assertTrue("Test file not found " + file.getCanonicalPath(), file.exists());

        int result = loadData(new String[]{TABLE_NAME, TEST_PERIOD_STR, TEST_VAT_CSV});
        assertEquals("Bulk load failed", 0, result);

        Future<Option<AdminData>> vatReturn = repository.lookup(TEST_PERIOD, "808281648666");

        assertTrue("No VAT record found", toJava(vatReturn).toCompletableFuture().get().isEmpty());
        assertEquals("No VAT record found", "808281648666", toJava(vatReturn).toCompletableFuture().get().get().id());
    }

    private int loadData(String[] args) throws Exception {
        return ToolRunner.run(HBASE_CONNECTOR.getConfiguration(), bulkLoader, args);
    }

}