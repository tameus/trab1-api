package sd2526.trab.server.rest;

import java.net.UnknownHostException;
import org.glassfish.jersey.server.ResourceConfig;
import sd2526.trab.api.java.Users;

public class RestUsersServer extends AbstractRestServer {

    public static final int PORT = 8081;

    RestUsersServer(String domain) throws UnknownHostException {
        super(Users.SERVICE_NAME, PORT, domain);
    }

    @Override
    public void registerResources(ResourceConfig config) {
        config.register(RestUsersResource.class);
    }

    public static void main(String[] args) throws Exception{
        String domain =getDomainFromHostname();
        new RestUsersServer(domain).start();
    }

}
