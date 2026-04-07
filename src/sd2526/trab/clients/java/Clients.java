package sd2526.trab.clients.java;

import java.net.URI;

import sd2526.trab.api.java.Users;
import sd2526.trab.api.java.Messages;
//import sd2526.trab.clients.grpc.GrpcUsersClient;
import sd2526.trab.clients.rest.RestUsersClient;
import sd2526.trab.clients.rest.RestMessagesClient;

public class Clients {
    //depois trocar o uri -> null por GrpcUsersClient::new
    public static final ClientFactory<Users> UsersClient = new GenericClientFactory<Users>( Users.SERVICE_NAME, RestUsersClient::new, uri -> null );

    public static final ClientFactory<Messages> MessagesClient = new GenericClientFactory<Messages>( Messages.SERVICE_NAME, RestMessagesClient::new, uri -> null );

    /*public static final ClientFactory<Users> UsersClientToo = new AbstractClientFactory<Users>( Users.SERVICE_NAME ) {

        Users createRestClient(URI serverURI) {
            return new RestUsersClient(serverURI);
        }

        Users createGrpcClient(URI serverURI) {
            return new GrpcUsersClient(serverURI);
        }
    };*/
}
