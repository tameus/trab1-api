package sd2526.trab.server.rest;

import java.net.URI;
import java.net.UnknownHostException;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import sd2526.trab.Discovery;
import sd2526.trab.server.AbstractServer;


public abstract class AbstractRestServer extends AbstractServer {
    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
    }

    protected AbstractRestServer(String service, int port, String domain) throws UnknownHostException {
        super(service, port, domain, "http", "rest");
    }
    @Override
    public void start() {
        try{
            this.discovery = new Discovery(Discovery.DISCOVERY_ADDR, "%s@%s".formatted(service,domain),serverURI);
            ResourceConfig config = new ResourceConfig();
            //binder para passar o domain
            config.register(new AbstractBinder() {
                @Override
                protected void configure() {
                    bind(domain).to(String.class).named("domain");
                    bind(discovery).to(Discovery.class);
                }
            });


            registerResources( config );

            var uri = URI.create("http://0.0.0.0:%s/rest".formatted(port));
            System.out.println( uri );
            JdkHttpServerFactory.createHttpServer( uri, config);

            discovery.start();

            Log.info(String.format("%s Server ready @ %s\n",  service, serverURI));

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public abstract void registerResources(ResourceConfig config);
}
