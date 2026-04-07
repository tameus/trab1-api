package sd2526.trab.clients.rest;

import java.net.URI;
import java.util.List;
import java.util.logging.Logger;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import sd2526.trab.api.User;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Users;
import sd2526.trab.api.rest.RestUsers;

public class RestUsersClient extends RestClient implements Users {

    private static Logger Log = Logger.getLogger(RestUsersClient.class.getName());

    public RestUsersClient( URI serverURI ) {
        super( serverURI, Log );

        target = super.target.path( RestUsers.PATH );
    }

    @Override
    public Result<String> postUser(User user) {
        return super.reTry( () -> doPostUser( user ) );

    }
    @Override
    public Result<User> getUser(String name, String pwd) {
        return super.reTry( () -> doGetUser( name, pwd ));
    }

    @Override
    public Result<User> updateUser(String name, String pwd, User user) {
        return super.reTry( () -> doUpdateUser( name, pwd, user ));
    }

    @Override
    public Result<User> deleteUser(String name, String pwd) {
        return super.reTry( () -> doDeleteUser( name, pwd ));
    }

    @Override
    public Result<List<User>> searchUsers(String name, String pwd, String query) {
        return super.reTry( () -> doSearchUsers( name, pwd, query ));
    }

    @Override
    public Result<User> verifyUser(String name) {
        return super.reTry( () -> doVerifyUser( name ));

    }

    private Result<String> doPostUser(User user) {
        Response r = target.request()
                .accept( MediaType.APPLICATION_JSON)
                .post(Entity.entity(user, MediaType.APPLICATION_JSON));

        return super.processResponse(r, String.class);
    }


    private Result<User> doGetUser(String name, String pwd) {
        Response r = target.path( name )
                .queryParam(RestUsers.PWD, pwd)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get();

        return super.processResponse( r, User.class );
    }

    private Result<User> doUpdateUser(String name, String pwd, User user) {
        Response r = target.path( name )
                .queryParam(RestUsers.PWD, pwd)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .put(Entity.entity(user, MediaType.APPLICATION_JSON));

        return super.processResponse( r, User.class );

    }

    private Result<User> doDeleteUser(String name, String pwd) {
        Response r = target.path( name )
                .queryParam(RestUsers.PWD, pwd)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .delete();

        return super.processResponse( r, User.class );

    }

    private Result<List<User>> doSearchUsers(String name, String pwd, String query) {
        Response r = target.queryParam(RestUsers.NAME, name)
                .queryParam(RestUsers.PWD, pwd)
                .queryParam(RestUsers.QUERY, query)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get();
        return super.processResponse( r, new GenericType<List<User>>() {});

    }

    private Result<User> doVerifyUser(String name) {
        Response r = target.path(name).path("verify")
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get();
        return super.processResponse(r, User.class);
    }




}
