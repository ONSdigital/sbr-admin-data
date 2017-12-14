# HBase Setup

To install HBase, run the following command:

```shell
brew install hbase
```

Use the following commands to start/stop HBase:

```shell
start-hbase.sh
stop-hbase.sh
```

Create some folders for use by the hbase bulk loader

```shell
cd /
sudo mkdir -p user/<username>/hbase-staging
sudo chmod 777 user/<username>/hbase-staging
```

Test it works:

```shell
hbase shell
```