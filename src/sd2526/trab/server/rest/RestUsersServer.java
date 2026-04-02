package sd2526.trab.server.rest;

import java.net.UnknownHostException;
import java.util.logging.Logger;

import org.glassfish.jersey.server.ResourceConfig;

import sd2526.trab.api.java.Users;

public class RestUsersServer extends AbstractRestServer {

    public static final int PORT = 8080;

    private static Logger Log = Logger.getLogger(RestUsersServer.class.getName());

    private final String domain;
    RestUsersServer(String domain) throws UnknownHostException {
        super( Logger.getLogger(RestUsersServer.class.getName()), Users.SERVICE_NAME, PORT);
        this.domain=domain;
    }

    @Override
    void registerResources(ResourceConfig config) {
        config.register(new RestUsersResource(domain));
    }

    public static void main(String[] args) throws Exception{
        String domain = args[0];
        new RestUsersServer(domain).start(domain);
    }

}
