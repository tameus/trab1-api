package sd2526.trab.server.rest;

import java.util.List;
import java.util.logging.Logger;

import sd2526.trab.api.User;
import sd2526.trab.api.java.Users;
import sd2526.trab.api.rest.RestUsers;
import sd2526.trab.server.java.JavaUsers;

public class RestUsersResource extends RestResource implements RestUsers {

    private static Logger Log = Logger.getLogger(RestUsersResource.class.getName());

    final Users impl;

    public RestUsersResource() {
        impl = new JavaUsers();
    }

    @Override
    public String postUser(User user) {
        Log.info("postUser : " + user);

        return super.unwrapResultOrThrow( impl.postUser( user ) );
    }

    @Override
    public User getUser(String name, String pwd) {
        Log.info("getUser : user = " + name + "; pwd = " + pwd);

        return super.unwrapResultOrThrow( impl.getUser(name, pwd));
    }

    @Override
    public User updateUser(String name, String pwd, User user) {
        Log.info("updateUser : user = " + name + "; pwd = " + pwd + " ; userData = " + user);
        return super.unwrapResultOrThrow( impl.updateUser(name, pwd, user));
    }

    @Override
    public User deleteUser(String name, String pwd) {
        Log.info("deleteUser : user = " + name + "; pwd = " + pwd);

        return super.unwrapResultOrThrow( impl.deleteUser(name, pwd));
    }

    @Override
    public List<User> searchUsers(String name, String pwd, String query) {
        Log.info("searchUsers : pattern = " + query);

        return super.unwrapResultOrThrow( impl.searchUsers( name, pwd, query));
    }
}
