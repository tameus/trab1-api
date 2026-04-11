package sd2526.trab.server.grpc;

import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import sd2526.trab.api.java.Messages;
import sd2526.trab.server.AbstractServer;

import java.net.UnknownHostException;

public class GrpcMessagesServer extends AbstractServer {
    public static final int PORT = 8084;

    protected GrpcMessagesServer(String domain) throws UnknownHostException {
        super(Messages.SERVICE_NAME, PORT, domain, "grpc", "grpc");
    }

    @Override
    public void start() {
        try {

            this.discovery = new sd2526.trab.Discovery(sd2526.trab.Discovery.DISCOVERY_ADDR, "%s@%s".formatted(service, domain), serverURI);
            discovery.start();
            GrpcMessagesController stub = new GrpcMessagesController(domain, discovery);

            Server server = Grpc.newServerBuilderForPort(port, InsecureServerCredentials.create())
                    .addService(stub)
                    .build();


            Log.info(String.format("%s gRPC Server ready @ %s\n", service, serverURI));
            server.start().awaitTermination();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        String domain = getDomainFromHostname();
        new GrpcMessagesServer(domain).start();
    }
}