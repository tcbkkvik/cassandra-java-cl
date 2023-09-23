package comp.torcb;

import static org.junit.Assert.*;
import static comp.torcb.Utils.formatPr;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.*;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import com.datastax.oss.driver.api.querybuilder.select.Select;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unit test for simple App.
 */
@SuppressWarnings("CommentedOutCode")
public class AppTest {
    static final Logger logger = LoggerFactory.getLogger(AppTest.class);
    private static CqlSession session;

//    @Before
    @BeforeClass
    public static void before() {
        session = new SessionBuilder().build();
    }

//    @After
    @AfterClass
    public static void after() {
        session.close();
    }

    @Test
    public void version() {
        logger.info("version_queryBuilder..");
        ResultSet rs = session.execute("select release_version from system.local");
        Row row = rs.one();
        assertNotNull(row);
        String ver = row.getString("release_version");
        System.out.printf("Cassandra release_version: %s\n", ver);
    }

    @Test
    public void version_queryBuilder() {
        //The query builder is NOT a crutch to learn CQL
        //While the fluent API guides you, it does not encode every rule of the CQL grammar.
        // Also, it supports a wide range of Cassandra versions, some of which may be more
        // recent than your production target, or not even released yet.
        Select select = QueryBuilder
                .selectFrom("system", "local")
                .column("release_version");
        logger.info("version_queryBuilder cql: " + select.asCql());
        ResultSet res = session.execute(select.build());
        formatPr(res);
    }

    @Test
    public void simpleQueries() {
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
        }
    }

    @Test
    public void executeAsync() {
        final String[] names = {"Bob", "Chris", "Mary" };
        //try (CqlSession session = newSession().build())
        {
            var drop = session.execute("""
                    DROP TABLE IF EXISTS users;""");
            formatPr(drop);
            var create = session.execute("""
                    CREATE TABLE users (
                      id int PRIMARY KEY,
                      name text
                    );""");
            formatPr(create);
            final String cql = "INSERT INTO users(id, name) VALUES (?,?)";
            PreparedStatement pst = session.prepare(cql);
            int okCount = 0;
            for (int id = 0; id < names.length; id++) {
                if (session
                        .execute(pst.bind(id, names[id]))
                        .wasApplied()) {
                    ++okCount;
                }
            }
            System.out.printf("%s\n -> ok: %d/%d\n", cql, okCount, names.length);
        }
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
        System.out.println("Count: " + count.get());
    }

    public static class Race {
        public int id, dist, participants;

        @SuppressWarnings("unused")
        public Race() {
        }

        public Race(int id, int dist, int participants) {
            this.id = id;
            this.dist = dist;
            this.participants = participants;
        }

        @Override
        public String toString() {
            return " {" +
                   "id=" + id +
                   ", dist=" + dist +
                   ", participants=" + participants +
                   '}';
        }
    }


/*  @Entity @CqlName("myKeySp.Race")
    static class Race2 {
        @PartitionKey public int id;
        public int dist, participants;
    }*/

    @Test
    public void jsonData() throws JsonProcessingException {
        formatPr(session.execute("DROP TABLE IF EXISTS myKeySp.Race"));
        formatPr(session.execute("""
                CREATE TABLE myKeySp.Race (
                  id int PRIMARY KEY,
                  dist int,
                  participants int
                )"""));
        final int[] distKm = {5, 10, 50, 100};
        var rnd = new Random();
        for (int id = 0; id < distKm.length; id++) {
            int count = rnd.nextInt(90);
            String json = id == 0
                    ? Utils.objMapper.writeValueAsString(new Race(id, distKm[id], count))
                    : """
                    { "dist":%d,"participants":%d,"id":%d}"""
                    .formatted(distKm[id], count, id);
            ResultSet res = session.execute(
                    "INSERT INTO myKeySp.Race JSON '" + json + "'");
            if (id == 0) formatPr(res);
        }

        ResultSet res = session.execute("SELECT * from myKeySp.Race");
        formatPr(res);

        String query = "SELECT JSON * from myKeySp.Race;";
        ResultSet resJson = session.execute(query);
        System.out.println(query);
        System.out.print(Utils.execInfo(resJson));
        for (Row row : resJson) {
            String json = row.getString(0);
            Race obj = Utils.objMapper.readValue(json, Race.class);
            System.out.println(obj);
        }
    }

    /* Key structures etc.:
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
