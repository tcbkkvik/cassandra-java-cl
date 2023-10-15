
************************************************
  Cassandra database server + Java CQL client;
  To get started with CQL client coding..
************************************************

Preconditions - recommended installations:
    * Docker Desktop >=4.21.1 ? (https://docs.docker.com/get-docker/)
        Or Rancher Desktop (https://rancherdesktop.io/)
        For running Cassandra server

    * IDE/Debugger: IntelliJIDEA or VSCode
        IntelliJ IDEA Community >=2022.3 ? https://www.jetbrains.com/idea/download/other.html

    * Java 17 - multiple sources, examples:
        https://openjdk.org/projects/jdk/17/
        https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html

    * Apache Maven >=3.8.5 ? (NB Maven comes bundled in IntelliJ IDEA Community)
        For building this Java Cassandra/CQL client
        https://maven.apache.org/download.cgi

-------------------------------
Getting started

1. Install recommended software listed above
2. Start Cassandra server:
    * Cd to directory where docker-compose.yml is (cassandra-java-cl/)
    * In docker-compose.yml, fix 'volumes:' path:
        "   volumes:
              - /_YOUR_ABSOLUTE_PATH/cassandra-java-cl/myDB:/var/lib/cassandra
        "
        (Or comment it out. On windows, docker seems to require absolute path to where your 'cassandra-java-cl' is)
    * run:
        > docker compose up -d
    * want until Cassandra server has started. You can check by querying docker logs:
        > docker compose logs
        > docker ps
        //expected row looks something like this:
        // db02c97715b4   cassandra:4.1.3   "docker-entrypoint.s…"   3 weeks ago    Up 29 hours   7001/tcp, 0.0.0.0:7000->7000/tcp, 7199/tcp, 0.0.0.0:9042->9042/tcp, 9160/tcp   mycassandra

3. Run unit tests in comp.torcb.AppTest (@Test..)
    All should succeed if Cassandra server succeeded starting.

4. Run unit tests in App2Test, and fix bugs..

5. Or, implement any Cassandra CQL client calls.
    Use this as basis.

-------------------------------

This Cassandra Maven/Docker project was initiated with:
> mvn archetype:generate -D groupId=comp.torcb -D artifactId=cassandra-java -D archetypeArtifactId=maven-archetype-quickstart -D archetypeVersion=1.4 -D interactiveMode=false

Github: https://github.com/tcbkkvik/cassandra-java-cl

Some docs:
 https://cassandra.apache.org/doc/4.1.3/cassandra/getting_started/index.html
 https://cassandra.apache.org/doc/4.1.3/cassandra/cql/types.html
 https://cassandra.apache.org/doc/4.1.3/cassandra/cql/json.html
 https://hub.docker.com/_/cassandra/
 https://www.datastax.com/blog/deep-look-cql-where-clause
 https://www.datastax.com/blog/allow-filtering-explained
 https://www.datastax.com/resources/whitepaper/data-modeling-apache-cassandra
 https://docs.datastax.com/en/developer/java-driver/4.17/manual/core/
 https://docs.datastax.com/en/developer/java-driver/4.17/manual/query_builder/
 https://docs.datastax.com/en/developer/java-driver/4.17/manual/mapper/
 https://docs.datastax.com/en/driver-matrix/docs/driver-matrix.html#quick_start/qsQuickstart_c.html
 https://www.odbms.org/wp-content/uploads/2014/06/WP-IntroToCassandra.pdf
 https://stackoverflow.com/questions/16870502/how-to-connect-cassandra-using-java-class
 https://github.com/datastaxdevs/workshop-intro-to-cassandra
 https://www.youtube.com/watch?v=yShQIKMt4sI
   (DS220.02 Relational Vs. Apache Cassandra | Data Modeling with Apache Cassandra)

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
  (NB edit/fix docker-compose.yml before running 'docker compose' (or 'docker-compose');
   Replace _______ with your absolute path (absolute if on Windows):
        volumes:
          - _______/cassandra-java-cl/myDB:/var/lib/cassandra
   Or alternatively, comment out  #volumes: and path below
  )
docker compose up --help
docker compose up -d
docker inspect -f '{{.NetworkSettings.IPAddress}}' mycassandra
docker inspect --format='{{range .NetworkSettings.Networks}}{{println .IPAddress}}{{end}}' mycassandra
 -- eg. 172.22.0.3
docker exec -i -t mycassandra bash -c ‘nodetool status’
docker exec -it mycassandra bash
docker compose stop
docker compose start

-------------------------------
Running CQL commands in bash -> cqlsh:

PS ..\cassandra-java>   docker exec -it mycassandra bash
root@de2015d844d3:/#    cqlsh
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