package hbase.util;

import model.AdminData;

import java.time.YearMonth;
import com.github.nscala_time.time.Imports.*;
import java.time.format.DateTimeFormatter;

public class RowKeyUtils {

    private static final String REFERENCE_PERIOD_FORMAT = "yyyyMM";
    static final String DELIMITER = "~";

    public static String getReferencePeriodFormat() {
        return REFERENCE_PERIOD_FORMAT;
    }


    public static String createRowKey(YearMonth referencePeriod, String id) {
        String period = referencePeriod.format(DateTimeFormatter.ofPattern(REFERENCE_PERIOD_FORMAT));
        return String.join(DELIMITER, period, id);
    }

    public static AdminData createAdminDataFromRowKey(String rowKey) {
        final String[] compositeRowKeyParts = rowKey.split(DELIMITER);
//        final YearMonth referencePeriod = YearMonth.parse(compositeRowKeyParts[0], DateTimeFormatter.ofPattern(REFERENCE_PERIOD_FORMAT));
        final YearMonth referencePeriod = YearMonth.parse(compositeRowKeyParts[0], DateTimeFormat.forPattern(REFERENCE_PERIOD_FORMAT)) ;
        /**
         * val referencePeriod: YearMonth =
         YearMonth.parse(compositeRowKeyParts.head, DateTimeFormat.forPattern(REFERENCE_PERIOD_FORMAT))
         */
        final String id = compositeRowKeyParts[1];
        return new AdminData(referencePeriod, id);
    }
}
