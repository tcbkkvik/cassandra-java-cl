package comp.torcb;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.servererrors.InvalidQueryException;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;
import java.util.function.Supplier;

import static comp.torcb.Utils.formatPr;
import static org.junit.Assert.*;

@SuppressWarnings({"SpellCheckingInspection", "DataFlowIssue"})
public class App2Test {
    private static CqlSession session;

    @BeforeClass
    public static void beforeClass() {
        session = new SessionBuilder().build();
    }

    @AfterClass
    public static void afterClass() {
        session.close();
    }

    @Test
    public void testMap() {
        session.execute("DROP TABLE IF EXISTS users");
        session.execute("""
                CREATE TABLE users (
                   id text PRIMARY KEY,
                   name text,
                   favs map<text, text> -- A map of text keys, and text values
                );
                """);
        session.execute("""
                INSERT INTO users (id, name, favs)
                   VALUES ('jsmith', 'John Smith', { 'fruit' : 'Apple', 'band' : 'Beatles' });
                """);
        String where = " WHERE id = 'jsmith'";
        Supplier<Map<String, String>> favs = () ->
                session.execute("SELECT * from users" + where)
                        .one().getMap("favs", String.class, String.class);
        var map = favs.get();
        System.out.println(map);
        assertEquals(2, map.size());

        // Replace the existing map entirely.
        session.execute("UPDATE users SET favs = { 'fruit' : 'Banana' }" + where);
        var map2 = favs.get();
        System.out.println(map2);
        assertFalse(map2.containsKey("band"));
        assertTrue(map2.containsKey("fruit"));

        //Updating or inserting one or more elements:
        session.execute("UPDATE users SET favs['author']= 'Ed Poe'" + where);
        session.execute("UPDATE users SET favs=favs+ {'movie':'Cassablanca', 'band':'ZZ Top'}" + where);
        var map3 = favs.get();
        System.out.println(map3);
        assertEquals(Map.of("author", "Ed Poe",
                "band", "ZZ Top",
                "fruit", "Banana",
                "movie", "Cassablanca"), map3);

        //Removing one or more element (if an element doesn’t exist, removing it is a no-op but no error is thrown):
        session.execute("DELETE favs['fruit'] FROM users" + where);
        session.execute("UPDATE users SET favs = favs - { 'movie', 'band'}" + where);
        var map4 = favs.get();
        System.out.println(map4);
//        assertEquals(1, map4.size());
        assertEquals(Map.of("fruit", "Banana"), map4);
    }

    @Test
    public void testSet() {
        session.execute("DROP TABLE IF EXISTS images");
        session.execute("""
                CREATE TABLE images (
                   name text PRIMARY KEY,
                   owner text,
                   tags set<text> // A set of text values
                )""");
        String where = " WHERE name = 'cat.jpg'";
        Supplier<Set<String>> select = () -> session
                .execute("SELECT tags FROM images" + where)
                .one().getSet(0, String.class);
        // Initial insert
        session.execute("""
                INSERT INTO images (name, owner, tags)
                   VALUES ('cat.jpg', 'jsmith', { 'pet', 'cute' })
                """);
        var set1 = select.get();
        assertEquals(Set.of("cute", "pet"), set1);

        // Replace the existing set entirely
        session.execute(" UPDATE images SET tags = { 'kitten', 'cat', 'lol' }" + where);
        var set2 = select.get();
        assertEquals(Set.of("cat", "kitten", "lol"), set2);

        // Add & remove
        session.execute("""
                BEGIN BATCH
                // Adding one or multiple elements (as this is a set, inserting an already existing element is a no-op):
                UPDATE images SET tags = { 'gray', 'cuddly' } %s;
                                
                //Removing one or multiple elements (if an element doesn’t exist
                //, removing it is a no-op but no error is thrown):
                UPDATE images SET tags = tags - { 'cat' } %s;
                APPLY BATCH;
                """.formatted(where, where));
        var set3 = select.get();
        assertEquals(Set.of("cuddly", "gray", "kitten", "lol"), set3);
    }

    @Test
    public void testList() {
        session.execute("DROP TABLE IF EXISTS plays");
        session.execute("""
                CREATE TABLE plays (
                    id text PRIMARY KEY,
                    game text,
                    players int,
                    scores list<int> // A list of integers
                )""");

        var resultSet = session.execute("INSERT INTO plays (id, game, players, scores)\n" +
                                        "  VALUES ('123-afde', 'quake', 3, [17, 4, 2])");
        formatPr(resultSet);
        final String where = " WHERE id = '123-afde'";
        Supplier<List<Integer>> select = () ->
                session.execute("SELECT scores FROM plays" + where)
                        .one().getList(0, Integer.class);
        var lst1 = select.get();
        assertEquals(List.of(17, 4, 2), lst1);

        // Replace the existing list entirely
        session.execute("UPDATE plays SET scores = [ 3, 9, 4]" + where);
        var lst2 = select.get();
        assertEquals(List.of(3, 9, 4), lst2);

        //Appending and prepending values to a list
        //Warning: Append and prepend are not idempotent by nature
        session.execute("UPDATE plays SET players = 5, scores = scores + [ 14, 21 ]" + where);
        session.execute("UPDATE plays SET players = 6, scores = [ 3 ] + scores" + where);
        var lst3 = select.get();
        assertEquals(List.of(3, 3, 9, 4, 14, 21), lst3);

        //Setting the value at a particular position:
        session.execute("UPDATE plays SET scores[1] = 7" + where);
        var lst4 = select.get();
        assertEquals(List.of(3, 7, 9, 4, 14, 21), lst4);

        //Removing:
        session.execute("DELETE scores[1] FROM plays" + where);
        var lst5 = select.get();
        assertEquals(List.of(3, 9, 4, 14, 21), lst5);

        //Deleting all the occurrences of particular values in the list (if a
        // particular element doesn’t occur at all in the list, it is simply
        // ignored and no error is thrown):
        session.execute("UPDATE plays SET scores = scores + [ 12, 21 ]" + where);
        var lst6 = select.get();
        assertEquals(List.of(3, 9, 4, 14), lst6);
    }

    @Test
    public void testClientSideJoin() throws JsonProcessingException {
        VideoSrv db = new VideoSrv(session).initTestTables();
        //~ videoOp JOIN video
        for (VideoSrv.Op op : db.loadVideoOps()) {
            for (int vidId : op.vids) {
                db.processVideo(op.op, db.loadVideo(vidId));
            }
        }
        System.out.println(db);
        assertEquals(db.processCountExpect, db.processCount);
        assertTrue(db.loadedMB < 2500);
    }

    @SuppressWarnings("unused")
    @Test
    public void testCompositeKey() {
        session.execute("DROP TABLE IF EXISTS ktab;");
        ResultSet createR = session.execute("""
                CREATE TABLE ktab(
                    p1 int,
                    p2 int,
                    c int,
                    f int,
                    PRIMARY KEY ((p1,p2), c)
                );""");
        PreparedStatement prep = session.prepare("INSERT INTO ktab (p1,p2,c,f) VALUES (?,?,?,?);");
        int[] vals = {1, 2};
        for (int p1 : vals) {
            for (int p2 : vals) {
                //p1,p2: composite partition key
                for (int c : new int[]{21, 7, 49}) {
                    //c: cluster key
                    for (int f : new int[]{70, 51, 103}) {
                        BoundStatement bound = prep.bind(p1, p2, c, f);
                        ResultSet insR = session.execute(bound);
                    }
                }
            }
        }
        try {
            formatPr(session.execute("SELECT * FROM ktab WHERE p2=1;"));
            //illegal -> CREATE INDEX?
        } catch (InvalidQueryException e) {
            System.out.println(e.getMessage());
            //Cannot execute this query as it might involve data filtering and thus
            // may have unpredictable performance. If you want to execute this query
            // despite the performance unpredictability, use ALLOW FILTERING
        }
        formatPr(session.execute("SELECT * FROM ktab WHERE p2=1 ALLOW FILTERING;"));
        //ORDER BY clauses can select a single column only
        formatPr(session.execute("SELECT * FROM ktab WHERE p1=1 and p2=1 ORDER BY c desc;"));
    }

    /*
        todo fix unit tests
        todo other?:
        - Rollback:
            Think of alternatives for compensation for
            missing Rollback functionality in Cassandra/CQL

        - Implement DAO mapping, using higher-level datastax mapper library.
            (com.datastax.oss:java-driver-mapper-runtime-4.17.0.jar)
            For example, by copying java example from javadoc in
               com.datastax.oss.driver.api.mapper.annotations.DaoFactory:
            " // Example 1: the session has a default keyspace
              CqlSession session = CqlSession.builder().withKeyspace("test").build();
              InventoryMapper inventoryMapper = new InventoryMapperBuilder(session).build();
              ProductDao dao = inventoryMapper.productDao();
              Product product = dao.selectById(1);
           "

        - CREATE INDEX: implement in unit test (see testCompositeKey() above)
            https://cassandra.apache.org/doc/4.1.3/cassandra/cql/indexes.html
            Eg. "CREATE INDEX userIndex ON NerdMovies (user);"

        - Evaluate: Is lack of OR operator a problem?
            https://cassandra.apache.org/doc/4.1.3/cassandra/cql/dml.html

    */
}
