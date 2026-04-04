package sd2526.trab.server.rest;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import sd2526.trab.Discovery;
import sd2526.trab.api.Message;
import sd2526.trab.api.java.Messages;
import sd2526.trab.api.rest.RestMessages;
import sd2526.trab.server.java.JavaMessages;

import java.util.List;
import java.util.logging.Logger;
@Singleton
public class RestMessagesResource extends RestResource implements RestMessages {

    final Messages impl;

    @Inject
    public RestMessagesResource(@Named("domain") String domain, Discovery discovery){
        super(domain, Logger.getLogger(RestMessagesResource.class.getName()));
        this.impl = new JavaMessages(domain,discovery);
    }
    @Override
    public String postMessage(String pwd, Message msg) {
        Log.info("postMessage : " + msg);

        return super.unwrapResultOrThrow( impl.postMessage( pwd,msg ) );
    }

    @Override
    public Message getMessage(String name, String mid, String pwd) {
        //Log.info("getMessage : user = " + name + "; pwd = " + pwd);

        return super.unwrapResultOrThrow( impl.getInboxMessage(name,mid, pwd));

    }

    @Override
    public List<String> getMessages(String name, String pwd, String query) {

        if (query == null || query.isBlank()){
            return super.unwrapResultOrThrow( impl.getAllInboxMessages(name, pwd));
        } else{
            return super.unwrapResultOrThrow( impl.searchInbox(name, pwd,query));

        }
    }

    @Override
    public void removeFromUserInbox(String name, String mid, String pwd) {

        super.unwrapResultOrThrow( impl.removeInboxMessage(name,mid,pwd));
    }

    @Override
    public void deleteMessage(String name, String mid, String pwd) {
        super.unwrapResultOrThrow( impl.deleteMessage( name, mid,pwd));

    }
}
