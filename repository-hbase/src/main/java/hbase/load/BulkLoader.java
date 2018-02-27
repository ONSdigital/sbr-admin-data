package hbase.load;

import hbase.connector.HBaseConnector;
import hbase.connector.HBaseInMemoryConnector;
import hbase.connector.HBaseInstanceConnector;
import hbase.model.AdminData;
import hbase.util.RowKeyUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.*;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.scalactic.Bool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * HBase bulk import example<br>
 * Data preparation MapReduce job driver
 * <ol>
 * <li>args[0]: table name
 * <li>args[1]: reference period, e.g. 201706
 * <li>args[2]: HDFS input path
 * <li>args[3]: primary key position in file
 * <li>args[4]: HDFS output path (optional)
 * </ol>
 */
public class BulkLoader extends Configured implements Tool {

    static final String REFERENCE_PERIOD = "hbase.load.period";
    static final String COLUMN_HEADINGS = "csv.column.headings";
    static final String ROWKEY_POSITION = "csv.id.position";
    public static final String REVERSE_FLAG = "load.format.reverse";
    public static final String HEADER_STRING = "csv.header.string";
    private static final int SUCCESS = 0;
    private static final int ERROR = -1;
    private static final int MIN_ARGS = 5;
    private static final int MAX_ARGS = 6;
    private static final int ARG_TABLE_NAME = 0;
    private static final int ARG_REFERENCE_PERIOD = 1;
    private static final int ARG_CSV_FILE = 2;
    private static final int ARG_CSV_ROWKEY_POSITION = 3;
    private static final int ARG_REVERSE_FLAG = 4;
    private static final int ARG_HFILE_OUT_DIR = 5;
    private static final int ARG_CSV_HEADER_STRING = 6;
    private static final Logger LOG = LoggerFactory.getLogger(BulkLoader.class);
    enum LoadCounters {
        TOTAL_CSV_RECORDS,
        GOOD_CSV_RECORDS,
        BAD_CSV_RECORDS,
        GOOD_HBASE_RECORDS,
        BAD_HBASE_RECORDS
    };

    private HBaseConnector connector;

    @Inject
    public BulkLoader(HBaseConnector connector) {
        this.connector = connector;
    }

    @Override
    public int run(String[] strings) throws Exception {
        if (strings == null || strings.length < MIN_ARGS || strings.length > MAX_ARGS) {
            System.out.println("INVALID ARGS, expected: table name, period, csv input file path, csv rowkey position, csv header string, hfile output path (optional)");
            System.exit(ERROR);
        }
        try {
            YearMonth.parse(strings[ARG_REFERENCE_PERIOD], DateTimeFormatter.ofPattern(AdminData.REFERENCE_PERIOD_FORMAT()));
            getConf().set(REFERENCE_PERIOD, strings[ARG_REFERENCE_PERIOD]);
        } catch (Exception e) {
            LOG.error("Cannot parse reference period with value '{}'. Format should be '{}'", strings[ARG_REFERENCE_PERIOD], AdminData.REFERENCE_PERIOD_FORMAT());
            System.exit(ERROR);
        }

        getConf().set(REVERSE_FLAG, strings[ARG_REVERSE_FLAG]);

        // Populate map reduce
        getConf().set(ROWKEY_POSITION, strings[ARG_CSV_ROWKEY_POSITION]);
        String headerArg = strings[ARG_CSV_HEADER_STRING];
        if (headerArg.isEmpty()) {
            Configuration conf = this.getConf();
            getHeader(strings[ARG_CSV_FILE], conf);
         } else {
            getConf().set(HEADER_STRING, strings[ARG_CSV_HEADER_STRING]);
         }

        if (strings.length == MIN_ARGS) {
            return (load(strings[ARG_TABLE_NAME], strings[ARG_REFERENCE_PERIOD], strings[ARG_CSV_FILE]));
        } else {
            return load(strings[ARG_TABLE_NAME], strings[ARG_REFERENCE_PERIOD], strings[ARG_CSV_FILE], strings[ARG_HFILE_OUT_DIR]);
        }
    }

    private int load(String tableNameStr, String referencePeriod, String inputFile) throws Exception {
        return load(tableNameStr, referencePeriod, inputFile, "");
    }

    private void getHeader(String inputFile, Configuration conf) throws Exception {
        Path path = new Path(inputFile);
        FileSystem fs = FileSystem.get(path.toUri(), conf);

        try(
                InputStreamReader reader = new InputStreamReader(fs.open(path));
                BufferedReader readFile = new BufferedReader(reader)
        ) {
            LOG.debug("Successfully read file at '{}' and now getting header.", inputFile);
            String header;
            header = readFile.readLine();
            if (header != null) {
                LOG.debug("Found header '{}'", header);
                conf.set(COLUMN_HEADINGS, header);
            }
            else {
                LOG.error("Header is NUll - for reading '{}'", inputFile);
                throw new Exception("Header is NUll - for reading " + inputFile);
            }
        } catch (Exception e) {
            LOG.error("Cannot process first line of file with exception '{}'", e);
            throw e;
        }
    }

    private int load(String tableNameStr, String referencePeriod, String inputFile, String outputFilePath) throws Exception {

        LOG.info("Starting bulk hbase.load of data from file {} into table '{}' for reference period '{}'", inputFile, tableNameStr, referencePeriod);

        // Time job
        Instant start = Instant.now();

        Job job;
        try {
            Connection connection = connector.getConnection();
            Configuration conf = this.getConf();
            final String namespace = System.getProperty("sbr.hbase.namespace", "");
            LOG.debug("Using namespace: {}", namespace);
            TableName tableName = TableName.valueOf(namespace, tableNameStr);
            Class<? extends Mapper> mapper;
            mapper = CSVDataKVMapper.class;
            job = Job.getInstance(conf, String.format("%s Admin Data Import", tableName));
            job.setJarByClass(mapper);
            job.setMapperClass(mapper);
            job.setMapOutputKeyClass(ImmutableBytesWritable.class);
            job.setMapOutputValueClass(Put.class);
            job.setInputFormatClass(TextInputFormat.class);
            FileInputFormat.setInputPaths(job, new Path(inputFile));

            // If we are writing HFiles
            if (!outputFilePath.isEmpty()) {
                try (Table table = connection.getTable(tableName)) {
                    try (RegionLocator regionLocator = connection.getRegionLocator(tableName)) {
                        {
                            job.setOutputFormatClass(HFileOutputFormat2.class);
                            job.setCombinerClass(PutCombiner.class);
                            job.setReducerClass(PutSortReducer.class);

                            // Auto configure partitioner and reducer
                            HFileOutputFormat2.configureIncrementalLoad(job, table, regionLocator);
                            Path hfilePath = new Path(String.format("%s%s%s_%s_%d", outputFilePath, Path.SEPARATOR, tableNameStr, referencePeriod, start.getEpochSecond()));
                            FileOutputFormat.setOutputPath(job, hfilePath);

                            if (job.waitForCompletion(true)) {
                                try (Admin admin = connection.getAdmin()) {
                                    // Load generated HFiles into table
                                    LoadIncrementalHFiles loader = new LoadIncrementalHFiles(conf);
                                    loader.doBulkLoad(hfilePath, admin, table, regionLocator);
                                }
                            } else {
                                LOG.error("Loading of data failed.");
                                return ERROR;
                            }
                        }
                    }
                }
            } else {
                TableMapReduceUtil.initTableReducerJob(tableName.getNameAsString(), null, job);
                job.setNumReduceTasks(0);
                if (!job.waitForCompletion(true)) {
                    LOG.error("Loading of data failed.");
                    return ERROR;
                }
            }

            boolean jobOK;
            long totalCsvCount = 0;
            long goodCsvCount = 0;
            long badCsvCount = 0;
            long goodHbaseCount = 0;
            long badHbaseCount = 0;
            long badHbaseWriteCount = 0;


            jobOK = job.waitForCompletion(true);
            totalCsvCount =  job.getCounters().findCounter(LoadCounters.TOTAL_CSV_RECORDS).getValue();
            System.out.println("Total csv records count " + totalCsvCount);
            goodCsvCount =  job.getCounters().findCounter(LoadCounters.GOOD_CSV_RECORDS).getValue();
            System.out.println("Good csv records count " + goodCsvCount);
            badCsvCount =  job.getCounters().findCounter(LoadCounters.BAD_CSV_RECORDS).getValue();
            System.out.println("Bad csv records count " + badCsvCount);
            goodHbaseCount =  job.getCounters().findCounter(LoadCounters.GOOD_HBASE_RECORDS).getValue();
            System.out.println("Good HBase records count " + goodHbaseCount);
            badHbaseCount =  job.getCounters().findCounter(LoadCounters.BAD_HBASE_RECORDS).getValue();
            System.out.println("Bad HBase records count " + badHbaseCount);


        } catch (Exception e) {
            LOG.error("Loading of data failed.", e);
            return ERROR;
        }

        Instant end = Instant.now();
        long seconds = Duration.between(start, end).getSeconds();
        LOG.info(String.format("Data loaded in %d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, (seconds % 60)));
        return SUCCESS;

    }

    public static void main(String[] args) {
        int result = 0;
        try {
            HBaseConnector connector = new HBaseInstanceConnector();
            result = ToolRunner.run(connector.getConfiguration(), new BulkLoader(connector), args);
            System.out.println("Bulkload success");
        } catch (Exception e) {
            result = ERROR;
            System.out.println("Bulkload error");
            e.printStackTrace();

        }
        System.exit(result);
    }
}

