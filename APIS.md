# Running the API's

* [sbr-api](https://github.com/ONSdigital/sbr-api):

```shell
sbt "api/run -Dhttp.port=9002 -Denvironment=local"
```

* [sbr-control-api](https://github.com/ONSdigital/sbr-control-api):

```shell
sbt "api/run -Dsbr.hbase.inmemory=true -Dhttp.port=9001"
```

* [sbr-admin-data](https://github.com/ONSdigital/sbr-admin-data):

```shell
sbt "sbr-admin-data/run"
```

* [business-index-api](https://github.com/ONSdigital/business-index-api):

```shell
elasticsearch
sbt "api/run -Denvironment=local"
```
