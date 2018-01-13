# sbr-admin-data
An API for use by sbr-api for accessing CompanyHouse/VAT/PAYE data

[![license](https://img.shields.io/github/license/mashape/apistatus.svg)]() [![Dependency Status](https://www.versioneye.com/user/projects/596f195e6725bd0027f25e93/badge.svg?style=flat-square)](https://www.versioneye.com/user/projects/596f195e6725bd0027f25e93)


### Prerequisites

* Java 8 or higher
* SBT ([Download](http://www.scala-sbt.org/))


### Development Setup (MacOS)

To install SBT quickly you can use Homebrew ([Brew](http://brew.sh)):
```shell
brew install sbt
```
Similarly we can get Scala (for development purposes) using brew:
```shell
brew install scala
```

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

## Running

To run the `sbr-admin-data`, run the following command:

``` shell
sbt run
```

By default, `sbr-admin-data` will look try to access data using HBase REST, which means you will have to have some data loaded into a local HBase table for this to work. You can find instructions for loading data into HBase in the [Loading Data section](#loading-data).

To run `sbr-admin-data` against an in memory HBase instance, run the following command:

```shell
sbt "run -DDB_IN_MEMORY=true"
```

## Environment Variables

| Environment Variable | Default Value   | Valid Values                                              |
|----------------------|-----------------|-----------------------------------------------------------|
| validation.id        | ".{3,8}"        | Any regex string for validating the id                    |
| cache.duration       | 60              | Any integer (number of minutes) for the cache duration    |
| cache.delimiter      | "~"             | Any string for the cache delimiter                        |
| cb.maxFailures       | 5               | Number of failures to change breaker to open state        |
| cb.callTimeout       | 2               | Number of seconds after which to timeout a request        |
| cb.resetTimeout      | 1               | Number of seconds after which the failure count is reset  |

## Loading Data

### Loading Data using the hbase shell

Use the following commands to load data into hbase:

```shell
start-hbase.sh
hbase shell
create_namespace 'sbr_local_db'
create 'sbr_local_db:admin_data', 'd'
put ‘sbr_local_db:admin_data’ , ’03007252~201706’, ’d:companynumber’, ’03007252’
put ‘sbr_local_db:admin_data’ , ’00032311~201706’, ’d:companynumber’, ’00032311’
```

Test the above commands works by doing a 'get', which should return a row of data.

```shell
hbase shell
get 'sbr_local_db:admin_data', '03007252~201706'
```

### Physical HBase Instance(hbase.in.memory.init = false)

To load data into a physical HBase instance


| Environment Variable | Default Value                  | Valid Values                                         |
|----------------------|--------------------------------|------------------------------------------------------|
| hbase.in.memory.init | true                           | false                                                |
| hbase.conf.dir       |                                | path to dir containing hbase-site.xml                |
| csv.header.string    |                                | string to be found in the header row of the csv file |

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
sbt -Dhbase.in.memory.init=false
    -Dcsv.header.string=companyname
    -Dhbase.conf.dir=/Users/myuser/hbase/conf
    repository-hbase/"run-main hbase.load.BulkLoader mytable 201706 /Users/myuser/data/ch-data.csv"
```


#### LocalHBase Setup

[HBase Setup](HBASE.md)

## Assembly

To package the project into a runnable fat-jar:

```shell
sbt assembly
```

## Deployment

After running the following command:
 
```shell
sbt clean compile "project api" universal:packageBin
```

A `.zip` file is created here, `/target/universal/sbr-admin-data-api.zip`, which is pushed to CloudFoundry in the deploy stage of the `Jenkinsfile`.

## Testing

To run all test suites we can use:

```shell
sbt test
```

Running an individual test can be specified by using the `testOnly` task, e.g.

```shell
sbt "project repository-hbase" "testOnly hbase.respository.HBaseAdminDataRepositoryScalaTest"
```


SBR Admin Data uses its own test configuration settings for integration tests, the details of which can be found on the [ONS Confluence](https://collaborate2.ons.gov.uk/confluence/display/SBR/Scala+Testing​).

To run integration tests execute the following command:

```shell
sbt it:test
```

See[CONTRIBUTING](CONTRIBUTING.md) for more details on creating tests.

## API Documentation
Swagger API is used to document and expose swagger definitions of the routes and capabilities for this project.

 To see the full definition set use path:
 `http://localhost:9000/swagger.json`
 
 For a graphical interface using Swagger Ui use path:
 `http://localhost:9000/docs`

## Contributing

See [CONTRIBUTING](CONTRIBUTING.md) for details.

## License

Copyright ©‎ 2017, Office for National Statistics (https://www.ons.gov.uk)

Released under MIT license, see [LICENSE](LICENSE) for details.
