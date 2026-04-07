package sd2526.trab.clients.java;

import sd2526.trab.api.java.Users;
import sd2526.trab.api.java.Messages;
//import sd2526.trab.clients.grpc.GrpcUsersClient;
import sd2526.trab.clients.grpc.GrpcUsersClient2;
import sd2526.trab.clients.rest.RestUsersClient;
import sd2526.trab.clients.rest.RestMessagesClient;

public class Clients {
    //TODO mudar para o grpcclients e nao o 2
    public static final ClientFactory<Users> UsersClient = new GenericClientFactory<Users>( Users.SERVICE_NAME, RestUsersClient::new, GrpcUsersClient2::new);

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
