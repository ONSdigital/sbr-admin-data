# sbr-admin-data
A bulk loader for parsing and loading CSV files into a datastore with a supporting API for single record retrieval for a period and primary key combination.
A "period" is defined as a year and month combination i.e. 201706
A primary key is a string value
The current implementation of the datastore is HBase

[![license](https://img.shields.io/github/license/mashape/apistatus.svg)]() [![Dependency Status](https://www.versioneye.com/user/projects/596f195e6725bd0027f25e93/badge.svg?style=flat-square)](https://www.versioneye.com/user/projects/596f195e6725bd0027f25e93)

## API Endpoints

If you do not specify a period, the record for the most recent period will be returned.

| method | endpoint                       | example                    |
|--------|--------------------------------|----------------------------|
| GET    | /v1/records/${id}              | GET /v1/records/AB123456   |


If you want to specify a particular period, use the format below.

| method | endpoint                                         | example                               |
|--------|--------------------------------------------------|---------------------------------------|
| GET    | /v1/periods/${period}/records/${id}              | /v1/periods/201706/records/AB123456   |


## Environment Setup

* Java 8 or higher (https://docs.oracle.com/javase/8/docs/technotes/guides/install/mac_jdk.html)
* SBT (http://www.scala-sbt.org/)

```shell
brew install sbt
```

## Running the API

With the minimal environment setup described above (just Java 8 and SBT), the sbr-admin-data-api will only work with the csv file or in-memory HBase. Further instructions for Hbase (not in memory), Hive and Impala setup/installations can be found [below](#source-setup).

To run the `sbr-admin-api`, run the following:

``` shell
sbt "sbr-admin-data/run"
```

### Running the API (database.in.memory = true)

By default the API will run against an in-memory HBase instance

| Environment Variable | Default Value                  | Valid Values                                         |
|----------------------|--------------------------------|------------------------------------------------------|
| database.in.memory   | true                           | true                                                 |
| hbase.table          | admin_data                     | any valid HBase table name                           |

When running with an in memory database a csv file will be loaded on startup. To configure that load further environment variables may be set.

| Environment Variable | Default Value                  | Valid Values                                         |
|----------------------|--------------------------------|------------------------------------------------------|
| csv.file             | conf/sample/201706/ch-data.csv | path to csv file to load                             |
| csv.header.string    |                                | string to be found in the header row of the csv file |

### Running the API (database.in.memory = false)

To run against a local HBase instance set the in-memory option to false

| Environment Variable | Default Value                  | Valid Values                                         |
|----------------------|--------------------------------|------------------------------------------------------|
| database.in.memory   | true                           | false                                                |
| hbase.table          | admin_data                     | any valid HBase table name                           |

## Loading Data

### Physical HBase Instance(database.in.memory = false)

To load data into a physical HBase instance


| Environment Variable | Default Value                  | Valid Values                                         |
|----------------------|--------------------------------|------------------------------------------------------|
| database.in.memory   | true                           | false                                                |
| hbase.conf.dir       |                                | path to dir containing hbase-site.xml                |
| csv.header.string    |                                | string to be found in the header row of the csv file |

Syntax (direct load)
```shell
sbt repository-hbase/"run-main hbase.load.BulkLoader {tablename} {period} {file}.csv"
```

Syntax (load via HFile)
```shell
sbt repository-hbase/"run-main hbase.load.BulkLoader {tablename} {period} {file}.csv {hfiledir}"
```


Example
```shell
sbt -Ddatabase.in.memory=false
    -Dcsv.header.string=companyname
    -Dhbase.conf.dir=/Users/myuser/hbase/conf
    repository-hbase/"run-main hbase.load.BulkLoader mytable 201706 /Users/myuser/data/ch-data.csv"
```


#### LocalHBase Setup

[HBase Setup](HBASE.md)

## Assembly

## Deployment

## Testing

## Contributing

See [CONTRIBUTING](CONTRIBUTING.md) for details.

## License

Copyright ©‎ 2017, Office for National Statistics (https://www.ons.gov.uk)

Released under MIT license, see [LICENSE](LICENSE) for details.
