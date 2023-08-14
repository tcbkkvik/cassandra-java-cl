package comp.torcb;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.internal.core.cql.DefaultSimpleStatement;

import java.util.UUID;

/**
 * Hello world!
 */
@SuppressWarnings("TextBlockMigration")
public class App {

        /*https://cassandra.apache.org/doc/latest/cassandra/cql/ddl.html
CREATE KEYSPACE IF NOT EXISTS myKeySp
   WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 3};
DESCRIBE KEYSPACE myKeySp;
USE myKeySp;

ALTER KEYSPACE excelsior
    WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 4};
DROP KEYSPACE excelsior;
:Dropping a keyspace results in the immediate, irreversible removal of that keyspace, including all the tables, user-defined types, user-defined functions, and all the data contained in those tables.

create_table_statement::= CREATE TABLE [ IF NOT EXISTS ] table_name '('
	column_definition  ( ',' column_definition )*
	[ ',' PRIMARY KEY '(' primary_key ')' ]
	 ')' [ WITH table_options ]

CREATE TABLE monkey_species (
    species text PRIMARY KEY,
    common_name text,
    population varint,
    average_size int
) WITH comment='Important biological records';

CREATE TABLE timeline (
    userid uuid,
    posted_month int,
    posted_time uuid,
    body text,
    posted_by text,
    PRIMARY KEY (userid, posted_month, posted_time)
) WITH compaction = { 'class' : 'LeveledCompactionStrategy' };



INSERT INTO NerdMovies (movie, director, main_actor, year)
   VALUES ('Serenity', 'Joss Whedon', 'Nathan Fillion', 2005)
   USING TTL 86400;

INSERT INTO NerdMovies JSON '{"movie": "Serenity", "director": "Joss Whedon", "year": 2005}';

         */

    private static void formatPr(ResultSet rs) {
        var req = rs.getExecutionInfo().getRequest();
        String query = ((DefaultSimpleStatement) req).getQuery();
        System.out.println(query);
        int n = 0;
        for (Row r : rs) {
            ++n;
            System.out.println(" -> " + r.getFormattedContents());
        }
        if (n == 0) {
            System.out.println(" -> " + (rs.wasApplied() ? "ok" : "failed; wasApplied==false"));
        }
        System.out.println();
    }

    void initSchemas(CqlSession session) {
//        int replication_factor = 3;
        int replication_factor = 1;
        ResultSet rsKS = session
                .execute("CREATE KEYSPACE IF NOT EXISTS myKeySp\n" +
                         " WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : "
                         + replication_factor + "};");
        formatPr(rsKS);
        ResultSet rsUse = session
                .execute("USE myKeySp;");
        formatPr(rsUse);
        ResultSet rsCreateTable = session
                .execute("CREATE TABLE IF NOT EXISTS t (" +
                         "  pk int,\n" +
                         "  ix int,\n" +
                         "  tx text,\n" +
                         "  st text static,\n" +
                         "  PRIMARY KEY (pk, ix)\n" +
                         ");");
        formatPr(rsCreateTable);

    }

    void accessDb(CqlSession session) throws InterruptedException {
        ResultSet rsIns1 = session
                .execute("INSERT INTO t (pk, ix, tx, st) VALUES (0, 2, 'val2', 'static2');");
        formatPr(rsIns1);
        ResultSet rsIns2 = session
                .execute("INSERT INTO t (pk, ix, tx, st) VALUES (0, 3, 'val3', 'static3');");
        formatPr(rsIns2);
        ResultSet rsSelect = session
                .execute("SELECT * FROM t;");
        formatPr(rsSelect);

        ResultSet rsCount = session
                .execute("SELECT COUNT (*) AS user_count FROM t where pk = 0;");
        formatPr(rsCount);
        ResultSet rsCreateCounter = session
                .execute("CREATE TABLE IF NOT EXISTS " +
                         "cnt (pk int PRIMARY KEY, cn counter);");
        formatPr(rsCreateCounter);
        for (int i = 0; i < 2; i++) {
            formatPr(session
                    .execute("UPDATE cnt SET cn=cn+1 WHERE pk=0;"));
            formatPr(session
                    .execute("SELECT * from cnt;"));
            Thread.sleep(10);
        }

    }

    void tryConnectCassandra() throws InterruptedException {
        var myClientId = UUID.randomUUID();
//        try (CqlSession session = new CqlSessionBuilder()
        try (CqlSession session = CqlSession.builder()
                .withApplicationName("learnCasApp")
                .withClientId(myClientId)
                .build()) {
            ResultSet rs = session.execute("select release_version from system.local");
            Row row = rs.one();
            if (row != null) {
                String ver = row.getString("release_version");
                System.out.printf("Cassandra release_version: %s\n", ver);
                initSchemas(session);
                accessDb(session);
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        new App().tryConnectCassandra();
    }
}
