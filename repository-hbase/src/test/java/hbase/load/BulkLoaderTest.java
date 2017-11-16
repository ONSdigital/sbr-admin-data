package hbase.load;

import akka.actor.ActorSystem;
import hbase.AbstractHBaseIT;
import hbase.repository.AdminDataExecutionContext;
import hbase.repository.HBaseAdminDataRepository;
import hbase.util.RowKeyUtils;
import model.AdminData;
import org.apache.hadoop.util.ToolRunner;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BulkLoaderTest extends AbstractHBaseIT {

    private static final String TEST_CSV_ID_COLUMN = "1";
    private static final YearMonth TEST_PERIOD = YearMonth.of(2017, Month.JUNE);
    private static final String TEST_PERIOD_STR = TEST_PERIOD.format(DateTimeFormatter.ofPattern(RowKeyUtils.getReferencePeriodFormat()));
    private static final String TEST_CH_CSV = "test/resources/ch-data.csv";
    private static final String TEST_PAYE_CSV = "test/resources/paye-data.csv";
    private static final String TEST_VAT_CSV = "test/resources/vat-data.csv";
    private BulkLoader bulkLoader;
    private static ActorSystem system;
    private HBaseAdminDataRepository repository;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create(AdminDataExecutionContext.NAME);
        System.setProperty(CSVDataKVMapper.ROWKEY_POSITION, TEST_CSV_ID_COLUMN);
    }


    @Before
    public void setUp() throws Exception {
        AdminDataExecutionContext context = new AdminDataExecutionContext(system);
        repository = new HBaseAdminDataRepository(HBASE_CONNECTOR, context);
        bulkLoader = new BulkLoader(HBASE_CONNECTOR);
    }

    @Test
    public void loadCompaniesData() throws Exception {
        System.setProperty(CSVDataKVMapper.HEADER_STRING, "companyname");
        File file = new File(TEST_CH_CSV);
        assertTrue("Test file not found " + file.getCanonicalPath(), file.exists());

        int result = loadData(new String[]{TABLE_NAME, TEST_PERIOD_STR, TEST_CH_CSV});
        assertEquals("Bulk load failed", 0, result);

        CompletionStage<Optional<AdminData>> company = repository.lookup(TEST_PERIOD, "04375380");
        assertTrue("No company registration found", company.toCompletableFuture().get().isPresent());

        assertEquals("No company registration found", "04375380", company.toCompletableFuture().get().get().getId());
    }

    @Test
    public void loadPAYEData() throws Exception {
        System.setProperty(CSVDataKVMapper.HEADER_STRING, "entref");
        File file = new File(TEST_PAYE_CSV);
        assertTrue("Test file not found " + file.getCanonicalPath(), file.exists());

        int result = loadData(new String[]{TABLE_NAME, TEST_PERIOD_STR, TEST_PAYE_CSV});
        assertEquals("Bulk load failed", 0, result);

        CompletionStage<Optional<AdminData>> payeReturn = repository.lookup(TEST_PERIOD, "8878574");
        assertTrue("No PAYE record found", payeReturn.toCompletableFuture().get().isPresent());

        assertEquals("No PAYE record found", "8878574", payeReturn.toCompletableFuture().get().get().getId());
    }

    @Test
    public void loadVATData() throws Exception {
        System.setProperty(CSVDataKVMapper.HEADER_STRING, "entref");
        File file = new File(TEST_VAT_CSV);
        assertTrue("Test file not found " + file.getCanonicalPath(), file.exists());

        int result = loadData(new String[]{TABLE_NAME, TEST_PERIOD_STR, TEST_VAT_CSV});
        assertEquals("Bulk load failed", 0, result);

        CompletionStage<Optional<AdminData>> vatReturn = repository.lookup(TEST_PERIOD, "808281648666");
        assertTrue("No VAT record found", vatReturn.toCompletableFuture().get().isPresent());

        assertEquals("No VAT record found", "808281648666", vatReturn.toCompletableFuture().get().get().getId());
    }

    private int loadData(String[] args) throws Exception {
        return ToolRunner.run(HBASE_CONNECTOR.getConfiguration(), bulkLoader, args);
    }

}