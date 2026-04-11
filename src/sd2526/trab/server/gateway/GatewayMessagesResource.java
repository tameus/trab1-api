package sd2526.trab.server.gateway;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import sd2526.trab.Discovery;
import sd2526.trab.api.Message;
import sd2526.trab.api.java.Messages;
import sd2526.trab.api.rest.RestMessages;
import sd2526.trab.clients.java.Clients;
import sd2526.trab.server.rest.RestResource;

import java.util.List;
import java.util.logging.Logger;
@Singleton
public class GatewayMessagesResource extends RestResource implements RestMessages {

    final Messages client;

    @Inject
    public GatewayMessagesResource(@Named("domain") String domain, Discovery discovery){
        super(domain, Logger.getLogger(GatewayMessagesResource.class.getName()));
        this.client = Clients.MessagesClient.get(domain, discovery);
    }

    @Override
    public String postMessage(String pwd, Message msg) {
        Log.info("Gateway encaminhando postMessage...");
        return super.unwrapResultOrThrow(client.postMessage(pwd, msg));
    }

    @Override
    public Message getMessage(String name, String mid, String pwd) {
        return super.unwrapResultOrThrow(client.getInboxMessage(name, mid, pwd));
    }

    @Override
    public List<String> getMessages(String name, String pwd, String query) {
        return super.unwrapResultOrThrow(client.searchInbox(name, pwd, query));
    }

    @Override
    public void removeFromUserInbox(String name, String mid, String pwd) {
        super.unwrapResultOrThrow(client.removeInboxMessage(name, mid, pwd));
    }

    @Override
    public void deleteMessage(String name, String mid, String pwd) {
        super.unwrapResultOrThrow(client.deleteMessage(name, mid, pwd));
    }

    @Override
    public String internalDeleteMessage(String mid) {
        return super.unwrapResultOrThrow(client.internalDeleteMessage(mid));
    }
}
