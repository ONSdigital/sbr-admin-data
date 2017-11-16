package hbase.util;

import model.AdminData;
import org.junit.Test;

import java.time.YearMonth;

import static org.junit.Assert.assertEquals;

public class RowKeyUtilsTest {

    private static final YearMonth TEST_REFERENCE_PERIOD = YearMonth.of(2017, 07);
    private static final String TEST_KEY = "123456789";
    private static final String[] TEST_PERIOD_KEY_COMPOSITE_KEY_PARTS = {"201707", TEST_KEY};
    private static final String TEST_VAT_ROWKEY = String.join(RowKeyUtils.DELIMITER, TEST_PERIOD_KEY_COMPOSITE_KEY_PARTS);

    @Test
    public void createRowKey() throws Exception {
        // Test generate row key form period + Strings
        String rowKey = RowKeyUtils.createRowKey(TEST_REFERENCE_PERIOD, TEST_KEY);
        assertEquals("Failure - row key not the same", TEST_VAT_ROWKEY, rowKey);
    }

    @Test
    public void createAdminDataFromRowKey() throws Exception {
        AdminData adminData = RowKeyUtils.createAdminDataFromRowKey(TEST_VAT_ROWKEY);
        assertEquals("Failure - key not the same", TEST_KEY, adminData.getId());
        assertEquals("Failure - reference period not the same", TEST_REFERENCE_PERIOD, adminData.getReferencePeriod());
    }

}