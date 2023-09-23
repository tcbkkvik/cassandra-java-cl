package comp.torcb;

import com.datastax.oss.driver.api.core.cql.*;
import com.datastax.oss.driver.api.core.metadata.Node;
import com.datastax.oss.driver.api.core.session.Request;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.util.Arrays;
import java.util.List;

public class Utils {
    public static final ObjectMapper objMapper = new ObjectMapper();
    public static final ObjectWriter objWriter;

    static {
//        objMapper.configure(JsonParser.Feature.IGNORE_UNDEFINED, true);
        objMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objWriter = objMapper.writerWithDefaultPrettyPrinter();
    }

    static String execInfo(ResultSet rs) {
        int no = -1;
        StringBuilder sb = new StringBuilder();
        List<ExecutionInfo> list = rs.getExecutionInfos();
        for (ExecutionInfo info : list) {
            ++no;
            sb.append(String.format(
                    "ExecInfo[%d] successIx:%d, bytes:%d:\n"
                    , no, info.getSuccessfulExecutionIndex()
                    , info.getResponseSizeInBytes()
            ));
            Node co = info.getCoordinator();
            if (co != null) {
                sb.append(String.format(
                        " Coordinator:%s, State:%s, Ver:%s\n"
                        , co.getEndPoint(), co.getState(), co.getCassandraVersion()
                ));
            }
/*to do??
            QueryTrace trace = info.getQueryTrace();
            sb.append(" Trace:\n");
            for (TraceEvent event : trace.getEvents()) {
                sb.append("  " + event.getActivity() + '\n');
            }
*/
        }
        return sb.toString();
    }

    static void formatPr(ResultSet rs) {
        Request req = rs.getExecutionInfo().getRequest();
        var sb = new StringBuilder();
        if (req instanceof SimpleStatement ss) {
            sb.append(ss.getQuery());
            var list = ss.getPositionalValues();
            if (!list.isEmpty())
                sb.append(Arrays.toString(list.toArray()));
            var map = ss.getNamedValues();
            if (!map.isEmpty()) {
                sb.append(map);
            }
            sb.append('\n');
//        } else if (req instanceof BoundStatement stm) {
//            sb.append(stm);
        }
        System.out.print(sb);
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
}
