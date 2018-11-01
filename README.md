# sbr-admin-data
This repository primarily serves two purposes, one which is as a bulk loader for parsing and loading CSV files into a data store - HBase.
The second usage derives from the core application folder which serves as a controller interface to make Restful requests to HBase using it the Rest client directly.

[![license](https://img.shields.io/github/license/mashape/apistatus.svg)]() [![Dependency Status](https://www.versioneye.com/user/projects/596f195e6725bd0027f25e93/badge.svg?style=flat-square)](https://www.versioneye.com/user/projects/596f195e6725bd0027f25e93)

## API Endpoints

If you do not specify a period, the record with matching id and the most recent period will be returned.

| method | endpoint                       | example                    |
|--------|--------------------------------|----------------------------|
| GET    | /v1/records/${id}              | GET /v1/records/AB123456   |

Alternatively, an id search with the a specified limit of results can be requested with:

| method | endpoint                              | example                                  |
|--------|---------------------------------------|------------------------------------------|
| GET    | /v1/records/${id}/history             | GET /v1/records/AB123456/history?max=2   |


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

To run the main project in this application, run the following:

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

#### Package

To package the project in a runnable fat-jar:
```shell
sbt assembly
```

#### Test

To test all test suites we can use:

```shell
sbt test
```

Testing an individual test suite can be specified by using the `testOnly`.

SBR Api uses its own test configuration settings for integration tests, the details of which can be found on the [ONS Confluence](https://collaborate2.ons.gov.uk/confluence/display/SBR/Scala+Testing).

To run integration test run:
```shell
sbt it:test
```
See [CONTRIBUTING](CONTRIBUTING.md) for more details on creating tests. 


#### API Documentation
Swagger API is used to document and expose swagger definitions of the routes and capabilities for this project.

 To see the full definition set use path:
 `http://localhost:9000/swagger.json`
 
 For a graphical interface using Swagger Ui use path:
 `http://localhost:9000/docs`

#### Application Tracing
[kamon](http://kamon.io) is used to automatically instrument the application and report trace spans to
[zipkin](https://zipkin.io/).

The AspectJ Weaver is required to make this happen, see [adding-the-aspectj-weaver](http://kamon.io/documentation/1.x/recipes/adding-the-aspectj-weaver/)
for further details.  Note that this is not currently activated when running tests.

To manually test, run a Zipkin 2 server.  The simplest way to do this is via docker:

    docker run --rm -d -p 9411:9411 openzipkin/zipkin:2.10.4

Then run the application via `sbt run`, and exercise an endpoint.
The trace information should be available in the Zipkin UI at
[http://localhost:9411/zipkin/](http://localhost:9411/zipkin/).
 
### Troubleshooting
See [FAQ](FAQ.md) for possible and common solutions.

### Contributing

See [CONTRIBUTING](CONTRIBUTING.md) for details.

### License

Copyright © 2017, Office for National Statistics (https://www.ons.gov.uk)

Released under MIT license, see [LICENSE](LICENSE.md) for details.
