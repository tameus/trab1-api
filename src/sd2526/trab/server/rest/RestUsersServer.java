package sd2526.trab.server.rest;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;

import org.glassfish.jersey.server.ResourceConfig;

import sd2526.trab.api.java.Users;

public class RestUsersServer extends AbstractRestServer {

    public static final int PORT = 8081;

    private static Logger Log = Logger.getLogger(RestUsersServer.class.getName());

    private final String domain;
    RestUsersServer(String domain) throws UnknownHostException {
        super( Logger.getLogger(RestUsersServer.class.getName()), Users.SERVICE_NAME, PORT);
        this.domain=domain;
    }

    @Override
    void registerResources(ResourceConfig config) {
        config.register(RestUsersResource.class);
        config.register(new org.glassfish.hk2.utilities.binding.AbstractBinder() {
            @Override
            protected void configure() {
                bind(domain).to(String.class).named("domain");
            }
        });
    }

    public static void main(String[] args) throws Exception{
        String domain ="mydomain"; //default
        String hostName = InetAddress.getLocalHost().getHostName();
        if(hostName.contains(".")){
            domain = hostName.substring(hostName.indexOf('.')+1);
        }

        new RestUsersServer(domain).start(domain);
    }

}
