package sd2526.trab.server.rest;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.logging.Logger;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import sd2526.trab.Discovery;


public abstract class AbstractRestServer {
    private static final String SERVER_BASE_URI = "http://%s:%s/rest";

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
    }


    final protected int port;
    final protected Logger Log;
    final protected String service;
    final protected String serverURI;
    final protected String domain;

    protected Discovery discovery;

    protected AbstractRestServer(String service, int port, String domain) throws UnknownHostException {
        this.port = port;
        this.service = service;
        this.domain = domain;
        this.Log = Logger.getLogger(this.getClass().getName());
        this.serverURI = SERVER_BASE_URI.formatted(InetAddress.getLocalHost().getHostAddress(), port);
    }

    protected static String getDomainFromHostname(){
        String domain ="mydomain"; //default
        try{
            String hostName = InetAddress.getLocalHost().getHostName();
            if(hostName.contains(".")){
                domain = hostName.substring(hostName.indexOf('.')+1);
            }
        }catch (UnknownHostException e){
            e.printStackTrace();
        }
        return domain;
    }

    protected void start() {
        try{
            ResourceConfig config = new ResourceConfig();
            //binder para passar o domain
            config.register(new AbstractBinder() {
                @Override
                protected void configure() {
                    bind(domain).to(String.class).named("domain");
                    bind(discovery).to(Discovery.class);
                }
            });

            this.discovery = new Discovery(Discovery.DISCOVERY_ADDR, "%s@%s".formatted(service,domain),serverURI);

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

    abstract void registerResources( ResourceConfig config );
}
