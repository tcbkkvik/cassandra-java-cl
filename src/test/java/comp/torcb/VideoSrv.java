package comp.torcb;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.Row;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("SpellCheckingInspection")
public class VideoSrv {
    CqlSession session;
    int processCountExpect;
    int processCount;
    int loadCount;
    int loadedMB;

    public VideoSrv(CqlSession session) {
        this.session = session;
    }

    public void resetCounts() {
        processCount = loadCount = loadedMB = 0;
    }

    public VideoSrv initTestTables() {
        session.execute("DROP TABLE IF EXISTS videoOp");
        session.execute("DROP TABLE IF EXISTS video");
        session.execute("""
                CREATE TABLE videoOp (
                  id int PRIMARY KEY,
                  op text,
                  vIds set<int>
                );""");
        session.execute("""
                CREATE TABLE video (
                  id int PRIMARY KEY,
                  title text,
                  sizeMB int,
                  payload text
                );""");
        for (var row : new String[]{
                "(1, 'compress',{1,2,4})",
                "(2, 'mp3Extract',{1,3})",
                "(3, 'makeCaption',{3,4})",
        }) {
            session.execute("INSERT INTO videoOp (id, op, vIds) VALUES " + row);
        }
        int id = 0;
        for (var entry : Map.of(
                "Black Sheep", 250,
                "Lost City", 1100,
                "Nope", 200,
                "Kung Fu Hustle", 700
        ).entrySet()) {
            session.execute("INSERT INTO video (id,title,sizeMB,payload)\n" +
                            "VALUES (?,?,?,'...')", (++id), entry.getKey(), entry.getValue());
        }
        return this;
    }

    public List<Op> loadVideoOps() throws JsonProcessingException {
        var list = new ArrayList<Op>();
        var rs = session.execute("SELECT JSON * FROM videoOp");
        processCountExpect = 0;
        for (Row row : rs) {
            String json = row.getString(0);
            Op e = Utils.objMapper.readValue(json, Op.class);
            processCountExpect += e.vids.size();
            list.add(e);
        }
        return list;
    }

    public Video loadVideo(int vId) throws JsonProcessingException {
        var json = session.execute("SELECT JSON * FROM video WHERE id = ?", vId)
                .one().getString(0);
        var video = Utils.objMapper.readValue(json, Video.class);
        ++loadCount;
        loadedMB += video.sizemb;
//        System.out.printf("loadVideoDataById %d MB\n", video.sizemb);
        return video;
    }

    public void processVideo(String op, Video video) {
        ++processCount;
//        System.out.println(String.format("Processed: %11s(%s)", op, video));
    }

    @Override
    public String toString() {
        return "Status{" +
               "loads=" + loadCount +
               ", MBytes=" + loadedMB +
               ", expectProcessed=" + processCountExpect +
               ", processed=" + processCount +
               '}';
    }

    public static class Video {
        public int id;
        public String title;
        public int sizemb;
        public String payload;

        public Video() {
        }

        @Override
        public String toString() {
            return String.format("%d '%14s' %4d MB %s", id, title, sizemb, payload);
        }
    }

    public static class Op {
        public int id;
        public String op;
        public Set<Integer> vids;

        public Op() {
        }
    }
}
