package comp.torcb;

import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.internal.core.cql.DefaultSimpleStatement;

public class Utils {
    static void formatPr(ResultSet rs) {
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
}
