package sd2526.trab.server.rest;

import java.net.UnknownHostException;
import org.glassfish.jersey.server.ResourceConfig;
import sd2526.trab.api.java.Messages;

public class RestMessagesServer extends AbstractRestServer{
    public static final int PORT = 8080;

    protected RestMessagesServer(String domain) throws UnknownHostException {
        super(Messages.SERVICE_NAME, PORT, domain);
    }

    @Override
    void registerResources(ResourceConfig config) {
        config.register(RestMessagesResource.class);

    }
    public static void main(String[] args) throws Exception{
        String domain = getDomainFromHostname(); //default
        new RestMessagesServer(domain).start();
    }
}
