package sd2526.trab.server.gateway;

import org.glassfish.jersey.server.ResourceConfig;
import sd2526.trab.server.rest.AbstractRestServer;

import java.net.UnknownHostException;

public class RestGatewayServer extends AbstractRestServer {
    public static final int PORT = 8082;
    protected RestGatewayServer(String domain) throws UnknownHostException {
        super("Gateway", PORT, domain);
    }

    @Override
    public void registerResources(ResourceConfig config) {
        config.register(GatewayUsersResource.class);
        config.register(GatewayMessagesResource.class);
    }

    public static void main(String[] args) throws Exception{
        String domain = getDomainFromHostname();
        new RestGatewayServer(domain).start();
    }
}
