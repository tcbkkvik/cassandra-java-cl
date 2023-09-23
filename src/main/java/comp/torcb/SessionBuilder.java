package comp.torcb;

import com.datastax.oss.driver.api.core.CqlSession;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.UUID;

import static comp.torcb.Utils.formatPr;

@SuppressWarnings("unused")
public class SessionBuilder {
    static final Logger logger = LoggerFactory.getLogger(SessionBuilder.class);
    private String datacenter = "datacenter1";
    private int port = 9042;
    private ReplicationClass replicationClass = ReplicationClass.SimpleStrategy;
    private String keySpace = "myKeySp";
    private String timeout = "15 seconds";
    private int replicationFactor = 1;
    private String applicationName = "learnCasApp";

    @SuppressWarnings("unused")
    public enum ReplicationClass {
        //SELECT * FROM system_schema.keyspaces;
        SimpleStrategy,
        NetworkTopologyStrategy,
        LocalStrategy
    }

    public SessionBuilder datacenter(@NonNull String datacenter) {
        this.datacenter = datacenter;
        return this;
    }

    public SessionBuilder port(int port) {
        this.port = port;
        return this;
    }

    public SessionBuilder replicationClass(@NonNull ReplicationClass replicationClass) {
        this.replicationClass = replicationClass;
        return this;
    }

    public SessionBuilder keySpace(@NonNull String keySpace) {
        this.keySpace = keySpace;
        return this;
    }

    public SessionBuilder timeoutSeconds(int sec) {
        this.timeout = String.format("%d seconds", sec);
        return this;
    }

    public SessionBuilder replicationFactor(int factor) {
        this.replicationFactor = factor;
        return this;
    }

    public SessionBuilder applicationName(@NonNull String applicationName) {
        this.applicationName = applicationName;
        return this;
    }

    public CqlSession build() {
        Properties pr = System.getProperties();
        pr.setProperty("datastax-java-driver.basic.request.timeout", timeout);
        UUID clientId = UUID.randomUUID();
        logger.info("newSession, cliendId " + clientId);
        CqlSession session = CqlSession.builder()
                .withApplicationName(applicationName)
                .withClientId(clientId)
                .withLocalDatacenter(datacenter)
//                .withKeyspace(keySpace)
                .addContactPoint(new InetSocketAddress(port))
                .build();
        session.execute(
                "CREATE KEYSPACE IF NOT EXISTS " + keySpace +
                " WITH replication = {" +
                "'class': '" + replicationClass + "', " +
                "'replication_factor' : " + replicationFactor +
                "};");
        formatPr(session.execute("USE " + keySpace));
        return session;
    }

}
