package sd2526.trab.server.java;

import sd2526.trab.Discovery;
import sd2526.trab.api.Message;
import sd2526.trab.api.java.Messages;
import sd2526.trab.api.User;
import sd2526.trab.api.java.Users;
import sd2526.trab.api.java.Result;
import sd2526.trab.clients.java.Clients;
import sd2526.trab.server.persistence.Hibernate;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class JavaMessages implements Messages {

    private static final String AT = "@";
    private static final String SENDER = "%s <%s@%s>";
    private static final String SENDER_EMAIL = "<%s@%s>";
    private static final long DELETE_TIMEOUT = 30000;

    private static final String FAILED_DELIVERY = "FAILED TO SEND %s TO %s: UNKNOWN USER";
    private static final String SELECT_INBOX_ENTRY = "SELECT i FROM Inbox i WHERE i.userAddress = '%s' AND i.messageId = '%s'";
    private static final String SELECT_MESSAGE = "SELECT m FROM Message m LEFT JOIN FETCH m.destination WHERE m.id = '%s'";
    private static final String SELECT_INBOX_IDS = "SELECT i.messageId FROM Inbox i WHERE i.userAddress = '%s'";
    private static final String SELECT_INBOX_BY_MID = "SELECT i FROM Inbox i WHERE i.messageId = '%s'";
    private static final String SEARCH_INBOX = "SELECT i.messageId FROM Inbox i, Message m " +
            "WHERE i.messageId = m.id AND i.userAddress = '%s' " +
            "AND (LOWER(m.subject) LIKE LOWER('%%%s%%') OR LOWER(m.contents) LIKE LOWER('%%%s%%'))";
    private static final String OPN = "<";
    private static final String CLS = ">";
    private static final String TIMEOUT_MSG = "FAILED TO SEND %s TO %s: TIMEOUT";
    private static final long MAX_TIMEOUT = 90000;
    private static final long RETRY_DELAY = 500;
    private static final int MAX_PERSIST_RETRIES = 10;

    private static Logger Log = Logger.getLogger(JavaMessages.class.getName());

    private final String domain;
    private final Discovery discovery;
    private final Hibernate hibernate;
    private final Users usersClient;

    private static final ExecutorService threadPool = Executors.newCachedThreadPool();

    public JavaMessages(String domain, Discovery discovery) {
        this.domain = domain;
        this.discovery = discovery;
        this.hibernate = Hibernate.getInstance();
        this.usersClient = Clients.UsersClient.get(domain, discovery);
    }

    @Override
    public Result<String> postMessage(String pwd, Message msg) {
        Log.info("postMessage processando...");

        //campos nulos
        if (msg == null || msg.getSender() == null || msg.getDestination() == null || msg.getDestination().isEmpty()) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }

        String sender = msg.getSender();
        String[] senderParts = parseSender(sender);
        String senderName = senderParts[0];
        String senderDomain = senderParts[1];

        if (!sender.contains(OPN)) {
            //sender local: verifica-lo e atualizar msg
            Result<String> res = handleLocalSender(pwd, msg, senderName, senderDomain);
            if (!res.isOK()) return res;
            String mid = msg.getId();
            //obter destinos remotos
            Set<String> remoteDomains = new HashSet<>();
            for (String dest : msg.getDestination()) {
                if (!extractDomain(dest).equals(this.domain) && senderDomain.equals(this.domain))
                    remoteDomains.add(extractDomain(dest));
            }
            List<String> unknownDests = new ArrayList<>();
            //obter inboxes e msg + destinos desconhecidos
            Object[] toPersist = collectLocalDeliveries(msg, unknownDests);
            //persistir msg+inboxes
            Result<String> persistResult = persistWithRetry(mid, toPersist);
            //ver se da erro
            if (!persistResult.isOK()) return persistResult;
            //enviar notificacao ao sender por cada destido desconhecido
            for (String dest : unknownDests) notificationDelivery(msg, dest, FAILED_DELIVERY);
            //realizar a entrega remota
            for (String remoteDomain : remoteDomains) remoteDelivery(remoteDomain, pwd, msg);
            return Result.ok(mid);
        } else {
            //sender remoto
            if (msg.getId() == null || msg.getId().isBlank()) {
                return Result.error(Result.ErrorCode.BAD_REQUEST);
            }
            String mid = msg.getId();
            List<String> unknownDests = new ArrayList<>();
            //obter inboxes e msg + destinos desconhecidos
            Object[] toPersist = collectLocalDeliveries(msg, unknownDests);
            //persistir msg+inboxes
            synchronized (mid.intern()) {
                //mensagem foi apagada
                if (hibernate.get(DeletedMessage.class, mid) != null) 
                    return Result.ok(mid);
                //posta mensagem
                Result<String> persistResult = persistWithRetry(mid, toPersist);
                if (!persistResult.isOK()) return persistResult;
            }
            //notificar sender sobre destinos inexistentes
            for (String dest : unknownDests){
                notificationDelivery(msg, dest, FAILED_DELIVERY);
            } 
            return Result.ok(mid);
        }
    }

    @Override
    public Result<Message> getInboxMessage(String name, String mid, String pwd) {
        //badrequest
        if (name == null || name.isBlank() || mid == null || mid.isBlank() || pwd == null || pwd.isBlank()) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }
        //forbidden, validar user
        Result<User> res = usersClient.getUser(name, pwd);
        if (!res.isOK()) {
            return Result.error(res.error());
        }

        //notfound
        try {
            //verificar se a mensagem é do user (mid no inbox dele)
            String query = String.format(SELECT_INBOX_ENTRY, name, mid);
            if (hibernate.jpql(query, Inbox.class).isEmpty()) {
                return Result.error(Result.ErrorCode.NOT_FOUND);
            }
            //ir busca-la
            Message msg = fetchMessage(mid);
            if (msg == null) {
                return Result.error(Result.ErrorCode.NOT_FOUND);
            }
            return Result.ok(msg);
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);

        }
    }

    @Override
    public Result<List<String>> getAllInboxMessages(String name, String pwd) {
        //badrequest
        if (name == null || name.isBlank() || pwd == null || pwd.isBlank()) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }
        //forbiden
        Result<User> res = usersClient.getUser(name, pwd);
        if (!res.isOK()) {
            return Result.error(res.error());
        }

        try {
            String query = String.format(SELECT_INBOX_IDS, name);
            List<String> msgIds = hibernate.jpql(query, String.class);
            return Result.ok(msgIds);
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<Void> removeInboxMessage(String name, String mid, String pwd) {
        //badrequest
        if (name == null || name.isBlank() || mid == null || mid.isBlank() || pwd == null || pwd.isBlank()) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }
        //forbiden
        Result<User> res = usersClient.getUser(name, pwd);
        if (!res.isOK()) {
            return Result.error(res.error());
        }

        try {
            String query = String.format(SELECT_INBOX_ENTRY, name, mid);
            List<Inbox> entries = hibernate.jpql(query, Inbox.class);
            //notfound
            if (entries.isEmpty()) {
                return Result.error(Result.ErrorCode.NOT_FOUND);
            }
            for (Inbox i : entries) {
                try {
                    hibernate.delete(i);
                } catch (Exception ignored) {}
            }
            return Result.ok();
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<Void> deleteMessage(String name, String mid, String pwd) {
        //badrequest
        if (name == null || name.isBlank() || mid == null || mid.isBlank() || pwd == null || pwd.isBlank()) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }
        //forbiden
        Result<User> res = usersClient.getUser(name, pwd);
        if (!res.isOK()) {
            return Result.error(res.error());
        }

        try {
            Message msg = fetchMessage(mid);
            if (msg == null) {
                //já está apagada ou nao existe
                return Result.ok();
            }

            //user=sender
            String expected = String.format(SENDER_EMAIL, name, this.domain);
            if (!msg.getSender().contains(expected)) {
                return Result.error(Result.ErrorCode.FORBIDDEN);
            }
            //30 segundos (esta em milisegundos)
            if ((System.currentTimeMillis() - msg.getCreationTime()) > DELETE_TIMEOUT) {
                return Result.error(Result.ErrorCode.FORBIDDEN);
            }
            //copia segura
            Set<String> destinations = Set.copyOf(msg.getDestination());
            executeDelete(msg);
            //para outros dominios
            remoteDelete(destinations, mid);
            return Result.ok();
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<List<String>> searchInbox(String name, String pwd, String query) {
        //badrequest
        if (name == null || name.isBlank() || pwd == null || pwd.isBlank()) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }
        //query vazia -> todas
        if (query == null || query.isBlank()) {
            return getAllInboxMessages(name, pwd);
        }
        //forbiden
        Result<User> res = usersClient.getUser(name, pwd);
        if (!res.isOK()) {
            return Result.error(res.error());
        }
        try {
            String sql = String.format(SEARCH_INBOX, name, query, query);
            return Result.ok(hibernate.jpql(sql, String.class));
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<String> internalDeleteMessage(String mid) {
        synchronized (mid.intern()) {
            try {
                hibernate.persist(new DeletedMessage(mid));
            } catch (Exception ignored) {}
            try {
                Message msg = fetchMessage(mid);
                if (msg != null) {
                    executeDelete(msg);
                }
            } catch (Exception e) {
                return Result.error(Result.ErrorCode.INTERNAL_ERROR);
            }
        }
        return Result.ok(mid);
    }

    private String extractName(String address) {
        return address.split(AT)[0];
    }
    private String extractDomain(String address) {
        int idx = address.indexOf(AT);
        if (idx<0) {
            return this.domain;
        }else{
            return address.substring(idx+1);
        }
    }

    private String[] parseSender(String sender) {
        String senderName, senderDomain;
        if (sender.contains(OPN) && sender.contains(CLS)) {
            senderName = sender.substring(sender.indexOf(OPN) + 1, sender.indexOf(AT));
            senderDomain = sender.substring(sender.indexOf(AT) + 1, sender.indexOf(CLS));
        } else {
            senderName = sender.split(AT)[0];
            if (sender.contains(AT)) {
                senderDomain = sender.split(AT)[1];
            } else {
                senderDomain = this.domain;
            }
        }
        return new String[]{senderName, senderDomain};
    }

    private Result<String> handleLocalSender(String pwd, Message msg, String senderName, String senderDomain) {
        //badrequest- sender local com outro dominio ou pass nula/vazia
        if (!senderDomain.equals(this.domain) || pwd == null || pwd.isBlank()) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }

        //forbidden- validar sender
        Result<User> res = usersClient.getUser(senderName, pwd);
        if (!res.isOK()) return Result.error(res.error());
        User senderUser = res.value();

        //gerar id da mensagem e tratar dominio
        String mid = msg.getId();
        if (mid == null || mid.isBlank()) {
            mid = UUID.randomUUID() + AT + this.domain; //1- msg nova
        } else if (!mid.contains(AT)) {
            mid = mid + AT + this.domain; //2- ja tem id, mas ainda nao tem o dominio (interrupcao)
        }//3- ja tem id e dominio- nao faz nada

        msg.setId(mid);
        msg.setSender(String.format(SENDER, senderUser.getDisplayName(), senderUser.getName(), senderUser.getDomain()));
        msg.setCreationTime(System.currentTimeMillis());
        return Result.ok();
    }

    private Object[] collectLocalDeliveries(Message msg, List<String> unknownDests) {
        String mid = msg.getId();
        List<Inbox> localInboxes = new ArrayList<>();
        for (String dest : msg.getDestination()) {
            if (extractDomain(dest).equals(this.domain)) {
                if (usersClient.verifyUser(extractName(dest)).isOK())
                    localInboxes.add(new Inbox(extractName(dest), mid));
                else
                    unknownDests.add(dest);
            }
        }
        //persistir mensagem + inboxes de uma so vez
        Object[] toPersist = new Object[localInboxes.size() + 1];
        toPersist[0] = msg;
        for (int i = 0; i < localInboxes.size(); i++){
            toPersist[i+1] = localInboxes.get(i);
        } 
        return toPersist;
    }

    private Result<String> persistWithRetry(String mid, Object[] toPersist) {
        for (int i = 0; i < MAX_PERSIST_RETRIES; i++) {
            try {
                //ja existe
                if (hibernate.get(Message.class, mid) != null) return Result.ok(mid);
                hibernate.persist(toPersist);
                return Result.ok(mid);
            } catch (Exception e) {
                if (hibernate.get(Message.class, mid) != null) return Result.ok(mid);
                Log.warning("Concurrency error (attempt " + (i + 1) + "): " + e.getMessage());
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            }
        }
        return Result.error(Result.ErrorCode.INTERNAL_ERROR);
    }

    private void notificationDelivery(Message msg, String dest, String subject){
        Log.info("entrei na notificationDelivery " + msg.getId());

        String [] parsed = parseSender(msg.getSender());
        String senderName = parsed[0];
        String senderDomain = parsed[1];
        Message notification = new Message();

        notification.setId(msg.getId()+"."+dest);

        notification.setSender(msg.getSender());

        Set<String> dests = new HashSet<>();
        dests.add(senderName + AT + senderDomain);
        notification.setDestination(dests);

        notification.setSubject(String.format(subject, msg.getId(), dest));
        notification.setContents(msg.getContents());
        notification.setCreationTime(System.currentTimeMillis());

        try {
            hibernate.persist(notification);
            if (senderDomain.equals(this.domain)) {
                Log.info("Putting in inbox of " + senderName);
                hibernate.persist(new Inbox(senderName, notification.getId()));
            } else {
                Log.info("Sending notif to domain " + senderDomain);
                remoteDelivery(senderDomain, null, notification);
            }
        } catch (Exception e) {
            Log.severe("Failure notification error for: " + e.getMessage());
        }
    }

    private void remoteDelivery(String destDomain, String pwd, Message msg) {
        submitWithRetry(
            //post messangem
            () -> Clients.MessagesClient.get(destDomain, this.discovery).postMessage(pwd, msg),
            //o que fazer caso dê timeout
            () -> {
                Log.warning("Gave up sending to " + destDomain + " after 90s.");
                for (String d : msg.getDestination())
                    if (d.endsWith(destDomain)) notificationDelivery(msg, d, TIMEOUT_MSG);
            }
        );
    }
    private Message fetchMessage(String mid) {
        String msgQuery = String.format(SELECT_MESSAGE, mid);
        List<Message> messages = hibernate.jpql(msgQuery, Message.class);
        if(messages.isEmpty())
            return null;
        return messages.get(0);
    }
    private void executeDelete(Message msg) {
        String ibxQuery = String.format(SELECT_INBOX_BY_MID, msg.getId());
        List<Inbox> inboxes = hibernate.jpql(ibxQuery, Inbox.class);
        //msg+inboxes num só
        List<Object> toDelete = new ArrayList<>(inboxes);
        toDelete.add(msg);
        try {
            hibernate.delete(toDelete.toArray());
        } catch (Exception ignored) {}
    }
    private void remoteDelete(Set<String> destination, String mid) {
        Set<String> remoteDomains = new HashSet<>();
        for (String dest : destination) {
            String destDomain = extractDomain(dest);
            if (!destDomain.equals(this.domain)) 
                remoteDomains.add(destDomain);
        }
        for (String remoteDomain : remoteDomains) {
            submitWithRetry(
                //apagar mensagem
                () -> Clients.MessagesClient.get(remoteDomain, this.discovery).internalDeleteMessage(mid),
                //nao acontece nada em timeout
                () -> {}
            );
        }
    }

private void submitWithRetry(Supplier<Result<String>> operation, Runnable onTimeout) {
        threadPool.submit(() -> {
            long startTime = System.currentTimeMillis();
            while ((System.currentTimeMillis() - startTime) < MAX_TIMEOUT) {
                try {
                    //realizar operaçao enquanto nao ha timeout
                    if (operation.get().isOK()) return;
                } catch (Exception ignored) {}
                try { Thread.sleep(RETRY_DELAY); } catch (InterruptedException ignored) {}
            }
            //açao caso dê timeout
            onTimeout.run();
        });
    }

}