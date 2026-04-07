package sd2526.trab.server.grpc;



import io.grpc.Server;
import io.grpc.ServerBuilder;
import sd2526.trab.Discovery;
import sd2526.trab.server.rest.AbstractRestServer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;

public class GrpcUsersServer  {

    private static final int PORT = 8091;

    public static void main(String[] args)  throws Exception {

        String domain = getDomainFromHostName();
        String ip = InetAddress.getLocalHost().getHostAddress();
        String serverUri = String.format("grpc://%s:%d", ip, PORT);

        Discovery discovery = new Discovery(Discovery.DISCOVERY_ADDR,"Users@" + domain, serverUri);
        discovery.start();
        Server server = io.grpc.ServerBuilder.forPort(PORT)

                .addService(new GrpcUsersResource(domain))
                .build();

        server.start();


        server.awaitTermination();
    }

    private static String getDomainFromHostName() {
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
}