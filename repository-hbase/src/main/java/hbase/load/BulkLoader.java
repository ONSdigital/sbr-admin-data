package hbase.load;

import hbase.connector.HBaseConnector;
import hbase.connector.HBaseInMemoryConnector;
import hbase.util.RowKeyUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
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

    static final String REFERENCE_PERIOD = "load.period";
    private static final int SUCCESS = 0;
    private static final int ERROR = -1;
    private static final int MIN_ARGS = 3;
    private static final int MAX_ARGS = 4;
    private static final int ARG_TABLE_NAME = 0;
    private static final int ARG_REFERENCE_PERIOD = 1;
    private static final int ARG_CSV_FILE = 2;
    private static final int ARG_HFILE_OUT_DIR = 3;
    private static final Logger LOG = LoggerFactory.getLogger(BulkLoader.class);

    private HBaseConnector connector;

    @Inject
    public BulkLoader(HBaseConnector connector) {
        this.connector = connector;
    }

    @Override
    public int run(String[] strings) throws Exception {
        if (strings == null || strings.length < MIN_ARGS || strings.length > MAX_ARGS) {
            System.out.println("INVALID ARGS, expected: table name, period, csv input file path, hfile output path (optional)");
            System.exit(ERROR);
        }
        try {
            YearMonth.parse(strings[ARG_REFERENCE_PERIOD], DateTimeFormatter.ofPattern(RowKeyUtils.REFERENCE_PERIOD_FORMAT()));
            System.setProperty(REFERENCE_PERIOD, strings[ARG_REFERENCE_PERIOD]);
        } catch (Exception e) {
            LOG.error("Cannot parse reference period with value '{}'. Format should be '{}'", strings[ARG_REFERENCE_PERIOD], RowKeyUtils.REFERENCE_PERIOD_FORMAT());
            System.exit(ERROR);
        }
        // Parse table name
        String tableName = strings[ARG_TABLE_NAME];
        // Check input file exists
        File f = new File(strings[ARG_CSV_FILE]);
        if (!f.exists() || f.isDirectory()) {
            LOG.error("CSV file does not exist or is a directory");
            System.exit(ERROR);
        }
        if (strings.length == MIN_ARGS) {
            return (load(strings[ARG_TABLE_NAME], strings[ARG_REFERENCE_PERIOD], strings[ARG_CSV_FILE]));
        } else {
            return load(strings[ARG_TABLE_NAME], strings[ARG_REFERENCE_PERIOD], strings[ARG_CSV_FILE], strings[ARG_HFILE_OUT_DIR]);
        }
    }

    private int load(String tableNameStr, String referencePeriod, String inputFile) {
        return load(tableNameStr, referencePeriod, inputFile, "");
    }

    private int load(String tableNameStr, String referencePeriod, String inputFile, String outputFilePath) {

        LOG.info("Starting bulk load of data from file {} into table '{}' for reference period '{}'", inputFile, tableNameStr, referencePeriod);

        // Time job
        Instant start = Instant.now();

        Job job;
        try {
            Connection connection = connector.getConnection();
            Configuration conf = this.getConf();
            TableName tableName = TableName.valueOf(tableNameStr);
            Class<? extends Mapper> mapper;
            mapper = CSVDataKVMapper.class;
            job = Job.getInstance(conf, String.format("SBR %s Admin Data Import", tableName));
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
                            FileOutputFormat.setOutputPath(job, new Path(String.format("%s%s%s_%s_%s", outputFilePath, File.pathSeparator, tableNameStr, referencePeriod, start.toString())));

                            if (job.waitForCompletion(true)) {
                                try (Admin admin = connection.getAdmin()) {
                                    // Load generated HFiles into table
                                    LoadIncrementalHFiles loader = new LoadIncrementalHFiles(conf);
                                    loader.doBulkLoad(new Path(outputFilePath), admin, table, regionLocator);
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
        try {
            HBaseConnector connector = new HBaseInMemoryConnector(args[1]);
            int result = ToolRunner.run(connector.getConfiguration(), new BulkLoader(connector), args);
            System.exit(result);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(ERROR);
        }
    }
}

