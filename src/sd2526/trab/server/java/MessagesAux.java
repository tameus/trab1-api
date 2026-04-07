package sd2526.trab.server.java;

import jakarta.ws.rs.core.MediaType;
import sd2526.trab.Discovery;
import sd2526.trab.api.Message;
import sd2526.trab.api.User;
import sd2526.trab.api.java.Messages;
import sd2526.trab.api.java.Result;
import sd2526.trab.server.persistence.Hibernate;
import sd2526.trab.api.User;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


public class MessagesAux {


    private static Logger Log = Logger.getLogger(JavaMessages.class.getName());

    private final String domain;
    private final Discovery discovery;
    private final Hibernate hibernate;


    public MessagesAux(String domain, Discovery discovery,Hibernate hibernate) {
        this.domain = domain;
        this.discovery = discovery;
        this.hibernate = hibernate; // A BD já está aqui pronta a usar!
    }
    public Result<User> checkUser(String sender, String pwd, String domain) {

        String clean = null;
        if (sender.contains("@")) {
            clean = sender.split("@")[0];
        } else {
            clean = sender;
        }
        clean.trim();

        var uris = discovery.knownUrisOf("Users@" + domain, 1);
        if (uris.length == 0) return Result.error(Result.ErrorCode.NOT_FOUND);

        try (var client = jakarta.ws.rs.client.ClientBuilder.newClient()) {
            var response = client.target(uris[0])
                    .path("users")
                    .path(clean)
                    .queryParam("pwd", pwd)
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .get();

            if (response.getStatus() == 200) {
                return Result.ok(response.readEntity(User.class));
            } else if (response.getStatus() == 403) {
                return Result.error(Result.ErrorCode.FORBIDDEN);
            }else if (response.getStatus() == 404) {
                return Result.error(Result.ErrorCode.NOT_FOUND);
            } else {
                return Result.error(Result.ErrorCode.INTERNAL_ERROR);
            }


        } catch (Exception e) {
            Log.severe("Erro ao contactar UsersServer: " + e.getMessage());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    public void failureNoti(String sender,Message msg,String failedDest){

        String senderName = null;
        if (sender.contains("<")) {
            senderName = sender.split("<")[1].split("@")[0];
        } else {
            senderName = sender.split("@")[0];
        }

        String mid = msg.getId();
        String notifyId = String.format("%s.%s",mid,failedDest);


        //parte do bonus
        //String subject = String.format("FAILED TO SEND %s TO %s TIMEOUT",mid,failedDest);
        java.util.Set<String> dests = new java.util.HashSet<>();
        dests.add(sender);

        Message notif = new Message(
                "Sistema@" +domain,
                sender,
                "FAILED TO SEND " + mid,
                msg.getContents()
        );
        notif.setDestination(dests);
        notif.setId(notifyId);
        notif.setCreationTime(System.currentTimeMillis());

        try {
            hibernate.persist(notif);

            hibernate.persist(new Inbox(senderName,notifyId));
            Log.info("Notificação de falha criada para " + sender + " com ID: " + notifyId);
        } catch (Exception e) {
            Log.severe("Erro ao persistir notificação: " + e.getMessage());
        }
    }
}
