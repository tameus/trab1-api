package sd2526.trab.server.grpc;

import java.net.UnknownHostException;

import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import sd2526.trab.api.java.Users;
import sd2526.trab.server.AbstractServer;

public class GrpcUsersServer extends AbstractServer {
    public static final int PORT = 8083;

    protected GrpcUsersServer(String domain) throws UnknownHostException {
        super(Users.SERVICE_NAME, PORT, domain, "grpc", "grpc");
    }

    @Override
    public void start() {
        try {
            GrpcUsersController stub = new GrpcUsersController(domain);
            Server server = Grpc.newServerBuilderForPort(port, InsecureServerCredentials.create())
                    .addService(stub)
                    .build();

            this.discovery = new sd2526.trab.Discovery(sd2526.trab.Discovery.DISCOVERY_ADDR, "%s@%s".formatted(service, domain), serverURI);
            discovery.start();

            Log.info(String.format("%s gRPC Server ready @ %s\n", service, serverURI));
            server.start().awaitTermination();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        String domain = getDomainFromHostname();
        new GrpcUsersServer(domain).start();
    }

}

