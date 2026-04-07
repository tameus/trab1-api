package sd2526.trab.clients.rest;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import sd2526.trab.api.Message;
import sd2526.trab.api.java.Messages;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.rest.RestMessages;

import java.net.URI;
import java.util.List;
import java.util.logging.Logger;

public class RestMessagesClient extends RestClient implements Messages {

    private static final Logger Log = Logger.getLogger(RestMessagesClient.class.getName());

    public RestMessagesClient(URI serverURI) {
        super(serverURI, Log);
        target = super.target.path(RestMessages.PATH);
    }

    @Override
    public Result<String> postMessage(String pwd, Message msg) {
        return super.reTry(() -> doPostMessage(pwd, msg));
    }

    @Override
    public Result<Message> getInboxMessage(String name, String mid, String pwd) {
        return super.reTry(() -> doGetInboxMessage(name, mid, pwd));
    }

    @Override
    public Result<List<String>> getAllInboxMessages(String name, String pwd) {
        return super.reTry(() -> doGetAllInboxMessages(name, pwd));
    }

    @Override
    public Result<Void> removeInboxMessage(String name, String mid, String pwd) {
        return super.reTry(() -> doRemoveInboxMessage(name, mid, pwd));
    }

    @Override
    public Result<Void> deleteMessage(String name, String mid, String pwd) {
        return super.reTry(() -> doDeleteMessage(name, mid, pwd));
    }

    @Override
    public Result<List<String>> searchInbox(String name, String pwd, String query) {
        return super.reTry(() -> doSearchInbox(name, pwd, query));
    }

    @Override
    public Result<String> internalDeleteMessage(String mid) {
        return super.reTry(() -> doInternalDeleteMessage(mid));
    }

    private Result<String> doPostMessage(String pwd, Message msg) {
        Response r = target.queryParam(RestMessages.PWD, pwd)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .post(Entity.entity(msg, MediaType.APPLICATION_JSON));
        return super.processResponse(r, String.class);
    }

    private Result<Message> doGetInboxMessage(String name, String mid, String pwd) {
        Response r = target.path(RestMessages.MBOX).path(name).path(mid)
                .queryParam(RestMessages.PWD, pwd)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get();
        return super.processResponse(r, Message.class);
    }

    private Result<List<String>> doGetAllInboxMessages(String name, String pwd) {
        Response r = target.path(RestMessages.MBOX).path(name)
                .queryParam(RestMessages.PWD, pwd)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get();
        return super.processResponse(r, new GenericType<List<String>>() {});
    }

    private Result<Void> doRemoveInboxMessage(String name, String mid, String pwd) {
        Response r = target.path(RestMessages.MBOX).path(name).path(mid)
                .queryParam(RestMessages.PWD, pwd)
                .request()
                .delete();
        return super.processResponse(r);
    }

    private Result<Void> doDeleteMessage(String name, String mid, String pwd) {
        Response r = target.path(name).path(mid)
                .queryParam(RestMessages.PWD, pwd)
                .request()
                .delete();
        return super.processResponse(r);
    }

    private Result<List<String>> doSearchInbox(String name, String pwd, String query) {
        Response r = target.path(RestMessages.MBOX).path(name)
                .queryParam(RestMessages.PWD, pwd)
                .queryParam(RestMessages.QUERY, query)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get();
        return super.processResponse(r, new GenericType<List<String>>() {});
    }

    private Result<String> doInternalDeleteMessage(String mid) {
        Response r = target.path("internal").path(mid)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .delete();
        return super.processResponse(r, String.class);
    }

}

