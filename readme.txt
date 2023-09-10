

This Cassandra Maven/Docker project was initiated with:
> mvn archetype:generate -D groupId=comp.torcb -D artifactId=cassandra-java -D archetypeArtifactId=maven-archetype-quickstart -D archetypeVersion=1.4 -D interactiveMode=false

Github: https://github.com/tcbkkvik/cassandra-java-cl

Some docs:
 https://cassandra.apache.org/doc/
 https://hub.docker.com/_/cassandra/
 https://www.datastax.com/blog/deep-look-cql-where-clause
 https://www.datastax.com/blog/allow-filtering-explained
 https://docs.datastax.com/en/developer/java-driver/4.17/manual/core/
 https://docs.datastax.com/en/developer/java-driver/4.17/manual/query_builder/
 https://docs.datastax.com/en/developer/java-driver/4.17/manual/mapper/
 https://docs.datastax.com/en/driver-matrix/docs/driver-matrix.html#quick_start/qsQuickstart_c.html
 https://stackoverflow.com/questions/16870502/how-to-connect-cassandra-using-java-class
 https://github.com/datastaxdevs/workshop-intro-to-cassandra

Maven:
 https://search.maven.org/artifact/com.datastax.oss/java-driver-core/4.17.0/jar?eh=
 https://central.sonatype.com/artifact/com.datastax.oss/java-driver-core/4.17.0?smo=true
  A driver for Apache Cassandra(R) 2.1+ that works exclusively with the Cassandra Query Language version 3 (CQL3) and Cassandra's native protocol versions 3 and above.

ScyllaDB - a Cassandra alternative (compatible with CQL & features):
 "ScyllaDB is a high-performance NoSQL database system, fully compatible with Apache Cassandra."
 https://www.scylladb.com/doc
 https://www.scylladb.com/open-source-nosql-database/
 https://lp.scylladb.com/real-time-big-data-database-principles-offer.html (white paper)
 https://hub.docker.com/r/scylladb/scylla
    docker exec -it myscylla bash
    nodetool help
    cqlsh


-------------------------------
A few key commands:
docker compose up
docker inspect -f '{{.NetworkSettings.IPAddress}}' mycassandra
docker inspect --format='{{range .NetworkSettings.Networks}}{{println .IPAddress}}{{end}}' mycassandra
 -- eg. 172.22.0.3
docker exec -i -t mycassandra bash -c ‘nodetool status’
docker exec -it mycassandra bash
docker compose stop
docker compose start

-------------------------------
Examples from running CQL commands in bash -> cqlsh:
PS C:\Users\torc\IdeaProjects\cassandra\cassandra-java> docker exec -it mycassandra bash
root@de2015d844d3:/# cqlsh
Connected to Test Cluster at 127.0.0.1:9042
[cqlsh 6.0.0 | Cassandra 4.0.11 | CQL spec 3.4.5 | Native protocol v5]
Use HELP for help.

cqlsh> DESCRIBE TABLES
    DESC KEYSPACES
    DESC CLUSTER
    DESC TYPES
    DESC AGGREGATES

cqlsh> CREATE KEYSPACE IF NOT EXISTS myKeySp    WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 3};
cqlsh> DESCRIBE KEYSPACE myKeySp;



-------------------------------
Output from App.java:

CREATE KEYSPACE IF NOT EXISTS myKeySp
 WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 1};
 -> ok

USE myKeySp;
 -> ok

CREATE TABLE IF NOT EXISTS t (  pk int,
  ix int,
  tx text,
  st text static,
  PRIMARY KEY (pk, ix)
);
 -> ok

INSERT INTO t (pk, ix, tx, st) VALUES (0, 2, 'val2', 'static2');
 -> ok

INSERT INTO t (pk, ix, tx, st) VALUES (0, 3, 'val3', 'static3');
 -> ok

SELECT * FROM t;
 -> [pk:0, ix:2, st:'static3', tx:'val2']
 -> [pk:0, ix:3, st:'static3', tx:'val3']

SELECT COUNT (*) AS user_count FROM t where pk = 0;
 -> [user_count:2]

CREATE TABLE IF NOT EXISTS cnt (pk int PRIMARY KEY, cn counter);
 -> ok

UPDATE cnt SET cn=cn+1 WHERE pk=0;
 -> ok

SELECT * from cnt;
 -> [pk:0, cn:1]

UPDATE cnt SET cn=cn+1 WHERE pk=0;
 -> ok

SELECT * from cnt;
 -> [pk:0, cn:2]

-------------------------------