package sd2526.trab.server.gateway;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import sd2526.trab.Discovery;
import sd2526.trab.api.User;
import sd2526.trab.api.java.Users;
import sd2526.trab.api.rest.RestUsers;
import sd2526.trab.clients.java.Clients;
import sd2526.trab.server.rest.RestResource;

import java.util.List;
import java.util.logging.Logger;
@Singleton
public class GatewayUsersResource extends RestResource implements RestUsers {
    final Users client;

    @Inject
    public GatewayUsersResource(@Named("domain") String domain, Discovery discovery) {
        super(domain, Logger.getLogger(GatewayUsersResource.class.getName()));
        this.client = Clients.UsersClient.get(domain, discovery);
    }

    @Override
    public String postUser(User user) {
        Log.info("Gateway postUser...");
        return super.unwrapResultOrThrow(client.postUser(user));
    }

    @Override
    public User getUser(String name, String pwd) {
        return super.unwrapResultOrThrow(client.getUser(name, pwd));
    }

    @Override
    public User updateUser(String name, String pwd, User info) {
        return super.unwrapResultOrThrow(client.updateUser(name, pwd, info));
    }

    @Override
    public User deleteUser(String name, String pwd) {
        return super.unwrapResultOrThrow(client.deleteUser(name, pwd));
    }

    @Override
    public List<User> searchUsers(String name, String pwd, String pattern) {
        return super.unwrapResultOrThrow(client.searchUsers(name, pwd, pattern));
    }

    @Override
    public User verifyUser(String name) {
        return super.unwrapResultOrThrow(client.verifyUser(name));
    }
}
