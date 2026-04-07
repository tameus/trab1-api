package sd2526.trab.server;

import sd2526.trab.Discovery;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;

public abstract class AbstractServer {
    final protected int port;
    final protected Logger Log;
    final protected String service;
    final protected String serverURI;
    final protected String domain;
    protected Discovery discovery;

    protected AbstractServer(String service, int port, String domain,String uriScheme, String context) throws UnknownHostException {
        this.port = port;
        this.service = service;
        this.domain = domain;
        this.Log = Logger.getLogger(this.getClass().getName());
        //http -> rest;  grpc -> grpc
        this.serverURI = String.format("%s://%s:%s/%s", uriScheme, InetAddress.getLocalHost().getHostAddress(), port, context);
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

    public abstract void start();
}
