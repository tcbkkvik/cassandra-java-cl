package comp.torcb;

import static org.junit.Assert.*;
import static comp.torcb.Utils.formatPr;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unit test for simple App.
 */
public class AppTest {
    static final Logger logger = LoggerFactory.getLogger(AppTest.class);
    private CqlSession session;

//    @BeforeClass public void commonInit() {}

    @Before
    public void init() {
        Properties pr = System.getProperties();
        pr.setProperty("datastax-java-driver.basic.request.timeout", "15 seconds");
        //Session
        session = newSession().build();
        initKeySpace(session);
    }

    private static void initKeySpace(CqlSession session) {
        int replication_factor = 1;
//        int replication_factor = 3;
        ResultSet rsKS = session
                .execute("CREATE KEYSPACE IF NOT EXISTS myKeySp\n" +
                         " WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : "
                         + replication_factor + "};");
        ResultSet rsUse = session.execute("USE myKeySp;");
        formatPr(rsUse);
    }

    private static CqlSessionBuilder newSession() {
        UUID clientId = UUID.randomUUID();
        logger.info("newSession, cliendId " + clientId);
        return CqlSession.builder()
                .withApplicationName("learnCasApp")
                .withClientId(clientId)
                .withLocalDatacenter("datacenter1")
//                .withKeyspace("myKeySp")
//                .withConfigLoader(new DefaultDriverConfigLoader())
                .addContactPoint(new InetSocketAddress(9042));
    }

    @After
    public void after() {
        session.close();
    }

    @Test
    public void version() {
        //Version
        ResultSet rs = session.execute("select release_version from system.local");
        Row row = rs.one();
        assertNotNull(row);
        String ver = row.getString("release_version");
        System.out.printf("Cassandra release_version: %s\n", ver);
    }

    @Test
    public void simpleQueries() throws InterruptedException {
        ResultSet rsCreateTable = session
                .execute("""
                        CREATE TABLE IF NOT EXISTS t (  pk int,
                          ix int,
                          tx text,
                          st text static,
                          PRIMARY KEY (pk, ix)
                        );""");
        formatPr(rsCreateTable);
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
                         "counters (pk int PRIMARY KEY, cn counter);");
        formatPr(rsCreateCounter);
        for (int i = 0; i < 2; i++) {
            formatPr(session
                    .execute("UPDATE counters SET cn=cn+1 WHERE pk=0;"));
            formatPr(session
                    .execute("SELECT * from counters;"));
            Thread.sleep(10);
        }
    }

    @Test
    public void executeAsync() throws InterruptedException {
        final String[] names = {"Alf", "Chris", "Irwin", "Mary" };
        //try (CqlSession session = newSession().build())
        {
//            initKeySpace(session);
            var drop = session.execute("""
                    DROP TABLE IF EXISTS users;""");
            formatPr(drop);
            Thread.sleep(300);
            var create = session.execute("""
                    CREATE TABLE users (
                      id int PRIMARY KEY,
                      name text
                    );""");
            formatPr(create);
            Thread.sleep(300);
            for (int id = 0; id < names.length; id++) {
                ResultSet ins = session.execute(String.format(
                        "INSERT INTO users(id, name) VALUES (%d, '%s');", id, names[id])
                );
                formatPr(ins);
            }
        }
        Thread.sleep(1000);
        final var count = new AtomicInteger();
        //try (CqlSession session = newSession().build())
        {
            PreparedStatement statement = session.prepare(
                    "SELECT * FROM myKeySp.users where id = ?");
            for (int id = 0; id < names.length; id++) {
                CompletionStage<AsyncResultSet> result = session.executeAsync(statement.bind(id));
                result.thenAccept(asyncResult -> {
                    for (Row r : asyncResult.currentPage()) {
                        count.incrementAndGet();
                        System.out.println(" -> " + r.getFormattedContents());
                    }
                });
            }
        }
        Thread.sleep(100);
        System.out.println("Count: " + count.get());
    }

    /* todo s
        //Primary key column
        CREATE TABLE users (
          userid text PRIMARY KEY,
          first_name text,
          last_name text,
          emails set<text>,
          top_scores list<int>,
          tasks map<timestamp, text>
        );
        //compound & composite primary key
        CREATE TABLE Cats (
          block_id uuid,
          breed text,
          color text,
          short_hair boolean,
          PRIMARY KEY ((block_id, breed), color, short_hair)
        ) WITH COMPACT STORAGE
          AND CLUSTERING ORDER BY (breed DESC);
        //Index
        CREATE INDEX ON _table (_col);
        //Where limitations
        SELECT * FROM _table
            WHERE _partition =  //OP: =, (_op <|>|<=|>= only with token(a) _op token(b))
            AND _cluster     =
            AND _dataField   =
            ALLOW FILTERING;
            //
        UPDATE+DELETE need all prim.keys (part+cluster cols in WHERE):
            Operators(=), except last Operators(=, IN)
    */
}
