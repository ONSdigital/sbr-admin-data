package hbase.load;

import au.com.bytecode.opencsv.CSVParser;
import hbase.model.AdminData;
import hbase.util.RowKeyUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import org.joda.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static hbase.load.BulkLoader.*;
import static org.apache.hadoop.hbase.util.Bytes.toBytes;

/**
 * hbase org.apache.hadoop.hbase.mapreduce.ImportTsv -Dimporttsv.separator=, -Dimporttsv.mapper.class=my.Mapper
 * <p>
 * hbase jar /Users/harrih/Git/sbr-hbase-connector/target/sbr-hbase-connector-1.0-SNAPSHOT-distribution.jar
 */
public class CSVDataKVMapper extends
        Mapper<LongWritable, Text, ImmutableBytesWritable, Put> {

    private static final Logger LOG = LoggerFactory.getLogger(CSVDataKVMapper.class.getName());
    private static final byte[] LAST_UPDATED_BY_COLUMN = toBytes("updatedBy");
    private static final byte[] LAST_UPDATED_BY_VALUE = toBytes("Data Load");
    private static final String COMMA = ",";

    private CSVParser csvParser;
    private YearMonth referencePeriod;
//    private byte[][] columnNames;
    private final byte[] columnFamily = toBytes("d");
    private int rowKeyFieldPosition;

    private String getHeaderString(Configuration conf) {
        return conf.get(HEADER_STRING, "");
    }

    private int getRowKeyFieldPosition(Configuration conf) {
        return conf.getInt(ROWKEY_POSITION, 1);
    }

    private boolean useCsvHeaderAsColumnNames(Configuration conf) {
        return conf.get(COLUMN_HEADINGS, "").isEmpty();
    }

    private byte[][] getColumnNames(Configuration conf) {
        String[] headings = conf.get(COLUMN_HEADINGS).split(COMMA);
        byte[][] columnNames = new byte[headings.length][];
        int i = 0;
        for (String heading : headings) {
            columnNames[i++] = toBytes(heading);
        }
        return columnNames;
    }

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        Configuration conf = context.getConfiguration();
        rowKeyFieldPosition = getRowKeyFieldPosition(conf);
        LOG.debug("Id field is a position {} in CSV file", rowKeyFieldPosition);
//        if (!useCsvHeaderAsColumnNames(conf)) {
//            LOG.debug("Using supplied column headers for file not those in the csv file");
//            columnNames = getColumnNames(conf);
//        }
        String periodStr = conf.get(REFERENCE_PERIOD);
        try {
            referencePeriod = YearMonth.parse(periodStr, DateTimeFormat.forPattern(AdminData.REFERENCE_PERIOD_FORMAT()));
        } catch (Exception e) {
            LOG.error("Cannot parse '{}' property with value '{}'. Format should be '{}'", REFERENCE_PERIOD, periodStr, AdminData.REFERENCE_PERIOD_FORMAT());
            throw e;
        }
//        if (getHeaderString(conf).isEmpty() && useCsvHeaderAsColumnNames(conf)){
//            LOG.error("If no header row identifying string is specified then column heading must be supplied");
//            throw new IllegalArgumentException("Property not set " + HEADER_STRING);
//        }
        csvParser = new CSVParser();
    }

    @Override
    protected void map(LongWritable key, Text value, Context context) throws InterruptedException, IOException {
        // Skip blank rows
        if (value == null || value.getLength() == 0) return;

        // Skip header
        if (isHeaderRow(value, context.getConfiguration())) return;

        context.getCounter(LoadCounters.TOTAL_CSV_RECORDS).increment(1);

        String[] fields = parseLine(value, context);
        if (fields == null) return;

        // Key: e.g. "201706~07382019"
        String rowKeyStr = RowKeyUtils.createRowKey(referencePeriod, fields[rowKeyFieldPosition]);
        writeRow(context, rowKeyStr, fields);
    }

    private boolean isHeaderRow(Text value, Configuration conf) throws IOException {
        String headerString = conf.get(COLUMN_HEADINGS);
        if (!headerString.isEmpty()) {
            if (value.find(headerString) > -1) {
//                if (useCsvHeaderAsColumnNames(conf)) {
//                    LOG.debug("Found csv header row: {}", value.toString());
//                    LOG.debug("Using header row as table column names");
//                    try {
//                        String[] columnNameStrs = csvParser.parseLine(value.toString().trim());
//                        List<byte[]> byteList = Arrays.stream(columnNameStrs).map(String::getBytes).collect(Collectors.toList());
//                        columnNames = byteList.toArray(new byte[0][0]);
//                    } catch (Exception e) {
//                        LOG.error("Cannot parse column headers, error is: {}", e);
//                        throw e;
//                    }
//                }
                return value.find(headerString) > -1;
            }
        }
        return false;
    }

    private String[] parseLine(Text value, Context context) {
        try {
            context.getCounter(LoadCounters.GOOD_CSV_RECORDS).increment(1);
            return csvParser.parseLine(value.toString());
        } catch (Exception e) {
            LOG.error("Cannot parse line '{}', error is: {}", value.toString(), e.getMessage());
            //context.getCounter(this.getClass().getSimpleName(), "PARSE_ERRORS").increment(1);
            context.getCounter(LoadCounters.BAD_CSV_RECORDS).increment(1);
            return null;
        }
    }

    private void writeRow(Context context, String rowKeyStr, String[] fields) throws IOException, InterruptedException {
        byte[][] columnNames = getColumnNames(context.getConfiguration());

        ImmutableBytesWritable rowKey = new ImmutableBytesWritable();

        rowKey.set(rowKeyStr.getBytes());
        Put put = new Put(rowKey.copyBytes());

        context.getCounter(LoadCounters.GOOD_HBASE_RECORDS).increment(1);

        for (int i = 0; i < fields.length; i++) {
            if (!fields[i].isEmpty()) {
                try {
                    put.add(new KeyValue(rowKey.get(), columnFamily, columnNames[i], fields[i].getBytes()));
                } catch (Exception e) {
                    LOG.error("Cannot add line '{}'", fields[0], e);
                    throw e;
                }
            }

        }

        // Add last updated column
        put.add(new KeyValue(rowKey.get(), columnFamily, LAST_UPDATED_BY_COLUMN, LAST_UPDATED_BY_VALUE));

        try {
            context.write(rowKey, put);
        } catch (Exception e) {
            context.getCounter(LoadCounters.BAD_HBASE_RECORDS).increment(1);
            LOG.error("Cannot write line '{}' to HFile", fields[0], e);
            throw e;
        }

    }

}
