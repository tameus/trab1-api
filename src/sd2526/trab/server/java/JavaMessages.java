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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class JavaMessages implements Messages {

    private static final String AT = "@";
    private static final String SENDER = "%s <%s@%s>";
    private static final String SENDER_EMAIL = "<%s@%s>";
    private static final long DELETE_TIMEOUT = 30000;

    private static final String FAILED_DELIVERY = "FAILED TO SEND %s TO %s: UNKNOWN USER";
    private static final String FAILED_ID = "%s.%s";
    private static final String SELECT_INBOX_ENTRY = "SELECT i FROM Inbox i WHERE i.userAddress = '%s' AND i.messageId = '%s'";
    private static final String SELECT_MESSAGE = "SELECT m FROM Message m LEFT JOIN FETCH m.destination WHERE m.id = '%s'";
    private static final String SELECT_INBOX_IDS = "SELECT i.messageId FROM Inbox i WHERE i.userAddress = '%s'";
    private static final String SELECT_INBOX_BY_MID = "SELECT i FROM Inbox i WHERE i.messageId = '%s'";
    private static final String SEARCH_INBOX = "SELECT i.messageId FROM Inbox i, Message m " +
            "WHERE i.messageId = m.id AND i.userAddress = '%s' " +
            "AND (LOWER(m.subject) LIKE LOWER('%%%s%%') OR LOWER(m.contents) LIKE LOWER('%%%s%%'))";
    private static final String OPN = "<";
    private static final String CLS = ">";
    private static final String TIMEOUT = "FAILED TO SEND %s TO %s: TIMEOUT";
    private static final long MAX_TIMEOUT = 90000;
    private static final long RETRY_DELAY = 500;

    private static Logger Log = Logger.getLogger(JavaMessages.class.getName());

    private final String domain;
    private final Discovery discovery;
    private final Hibernate hibernate;
    private final Users usersClient;
    //private final MessagesAux messAux;


    public JavaMessages(String domain, Discovery discovery) {
        this.domain = domain;
        this.discovery = discovery;
        this.hibernate = Hibernate.getInstance(); // A BD já está aqui pronta a usar!
        this.usersClient = Clients.UsersClient.get(domain, discovery);
        //this.messAux = new MessagesAux(domain,discovery,hibernate);

    }

    @Override
    public Result<String> postMessage(String pwd, Message msg) {
        Log.info("postMessage processando...");

        //campos nulos
        if (msg == null || msg.getSender() == null || msg.getDestination() == null || msg.getDestination().isEmpty()) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }

        boolean isNotification = msg.getId() != null && msg.getId().contains(".");

        String sender = msg.getSender();
        String[] senderParts = parseSender(sender);
        //String sender = msg.getSender();
        //vem com <name@domain>, local é name@domain ou só name
        String senderName = senderParts[0];
        String senderDomain = senderParts[1];
        boolean isForwarded = (sender.contains(OPN) && sender.contains(CLS))||isNotification;

        if (!isForwarded) {
            Result<String> res = handleLocalSender(pwd, msg, senderName, senderDomain);
            //dá erro -> devolve logo
            if (!res.isOK()) return res;
        } else {
            //já devia ter id, se vem de outro dominio
            if (msg.getId() == null || msg.getId().isBlank()) {
                return Result.error(Result.ErrorCode.BAD_REQUEST);
            }
        }

        int maxRetries = 10;
        int currentRetry = 0;
        boolean success = false;
        while (currentRetry < maxRetries && !success) {
            try {
                //testar idempotencia (so quando id != null), se ja existe nao faz nada
                if (hibernate.get(Message.class, msg.getId()) != null) {
                    return Result.ok(msg.getId());
                }
                //guardar mensagem na bd
                hibernate.persist(msg);
                success = true;
            } catch (Exception e) {
                if (hibernate.get(Message.class, msg.getId()) != null) {
                    return Result.ok(msg.getId());
                }
                currentRetry++;
                Log.warning("Concurrency error (attempt " + currentRetry + "): " + e.getMessage());
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {
                }
            }
        }
        if (!success) {
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        //enviar a cada destinatário
        sendToDestinations(msg, senderName, senderDomain, pwd);
        return Result.ok(msg.getId());
    }

    @Override
    public Result<Message> getInboxMessage(String name, String mid, String pwd) {
        //badrequest
        if (name == null || name.isBlank() || mid == null || mid.isBlank() || pwd == null || pwd.isBlank()) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }
        //var user = messAux.checkUser(name,pwd,this.domain);
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

        //apagar apenas da inbox do utilizador
        try {
            String query = String.format(SELECT_INBOX_ENTRY, name, mid);

            List<Inbox> entries = hibernate.jpql(query, Inbox.class);
            //notfound
            if (entries.isEmpty()) {
                return Result.error(Result.ErrorCode.NOT_FOUND);
            }
            try {
                hibernate.delete(entries.get(0));
            }catch(Exception ignored) {}
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
            /*Message msg = hibernate.get(Message.class,mid);
            //caso nao exista, nao acontece nada e da ok
            if(msg == null) {
                return Result.ok();
            }*/
            //user=sender
            String expected = String.format(SENDER_EMAIL, name, this.domain);
            if (!msg.getSender().contains(expected)) {
                return Result.error(Result.ErrorCode.FORBIDDEN);
            }
            //30 segundos (esta em milisegundos)
            if (System.currentTimeMillis() - msg.getCreationTime() > DELETE_TIMEOUT) {
                return Result.error(Result.ErrorCode.FORBIDDEN);
            }
            //copia segura
            Set<String> destinations = Set.copyOf(msg.getDestination());
            String messageId = msg.getId();
            //apagar inboxes (mid e nnao messageId)
            /*String ibxQuery = String.format(SELECT_INBOX_BY_MID, messageId);
            List<Inbox> inboxes = hibernate.jpql(ibxQuery,Inbox.class);
            for(Inbox i : inboxes){
                hibernate.delete(i);
            }

            //apagar inboxes
            String ibxQuery = String.format(DELETE_INBOX, messageId);
            hibernate.jpql(ibxQuery,Inbox.class);
            //apagar destinos
            String destQuery = String.format(DELETE_MESSAGE_DESTS, messageId);
            hibernate.jpql(destQuery,Message.class);

            //apagar mensagem
            hibernate.delete(msg);*/
            executeDelete(msg);
            //internalDeleteMessage(messageId);
            //para outros dominios
            remoteDelete(destinations, messageId);
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
        try {
            Message msg = fetchMessage(mid);
            if (msg == null) {
                return Result.ok(mid);
            }
            /*String ibxQuery = String.format(DELETE_INBOX, mid);
            hibernate.jpql(ibxQuery,Inbox.class);
            //apagar destinos
            String destQuery = String.format(DELETE_MESSAGE_DESTS, mid);
            hibernate.jpql(destQuery,Message.class);*/
            executeDelete(msg);
            return Result.ok(mid);
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
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
        boolean isForwarded = sender.contains(OPN) && sender.contains(CLS);
        String senderName, senderDomain;
        if (isForwarded) {
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
        if (!res.isOK()) {
            return Result.error(res.error());
        }
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

    private void sendToDestinations(Message msg, String senderName, String senderDomain, String pwd) {
        Set<String> remoteDomains = new HashSet<String>();
        for (String dest : msg.getDestination()) {
            //para cada mensagem analisar o destino
            String destName = extractName(dest);
            String destDomain = extractDomain(dest);
            if (destDomain.equals(this.domain)) {
                //enviar para utilizador local
                localDelivery(dest, destName, msg);
            } else if (senderDomain.equals(this.domain)) {
                //enviar para utilizador remoto, a partir deste dominio
                remoteDomains.add(destDomain);
            }
        }
        for (String remoteDomain : remoteDomains) {
            remoteDelivery(remoteDomain, pwd, msg);
        }
    }

    private void localDelivery(String dest, String destName, Message msg) {
        Result<User> res = usersClient.verifyUser(destName);
        if (res.isOK()) {
            hibernate.persist(new Inbox(destName, msg.getId()));
        } else {
            notificationDelivery(msg, dest, FAILED_DELIVERY);
        }
    }

    private void notificationDelivery(Message msg, String dest, String subject){
        Log.info("entrei na notificationDelivery " + msg.getId());

        String [] parsed = parseSender(msg.getSender());
        String senderName = parsed[0];
        String senderDomain = parsed[1];
        Message notification = new Message();
        //notification.setId(String.format(FAILED_ID, msg.getId(), extractName(dest)) + AT + this.domain);
        //notification.setId(String.format(FAILED_ID, msg.getId(), dest));
        notification.setId(msg.getId()+"."+dest);

        notification.setSender(msg.getSender());

        Set<String>dests =new HashSet<>();
        dests.add(senderName+ AT + senderDomain);
        notification.setDestination(dests);
        //notification.setDestination(Set.of(senderName + AT + senderDomain));

        notification.setSubject(String.format(subject, msg.getId(), dest));
        notification.setContents(msg.getContents());
        notification.setCreationTime(System.currentTimeMillis());
        Log.info("Prestes a entrar na thread: " + notification.getId());

        try {
            Log.info("A gravar Timeout Notif na DB local: " + notification.getId());
            hibernate.persist(notification);
            if (senderDomain.equals(this.domain)) {
                Log.info("A colocar na inbox de " + senderName);
                hibernate.persist(new Inbox(senderName, notification.getId()));
            } else {
                Log.info("A enviar notif para dominio " + senderDomain);
                remoteDelivery(senderDomain, null, notification);
            }
        } catch (Exception e) {
            Log.severe("Failure notification error for: " + e.getMessage());
        }
    }

    private void remoteDelivery(String destDomain, String pwd, Message msg) {
        AtomicBoolean success = new AtomicBoolean(false);
        AtomicBoolean timeIsUp = new AtomicBoolean(false);
        Timer timer = new Timer(true);

        timer.schedule(new TimerTask(){
            @Override
            public void run() {
                timeIsUp.set(true);

                if(!success.get()){
                    Log.warning("Gave up sending to " + destDomain + " after 90s.");
                    //String destUser = "";
                    for(String d : msg.getDestination()) {
                        if(d.endsWith(destDomain)){
                            //destUser = d;
                            //break;
                            notificationDelivery(msg, d, TIMEOUT);
                        }
                    }
                    /*if(!destUser.isEmpty()){
                        notificationDelivery(msg, destUser, TIMEOUT);
                    }*/
                }
            }
        }, MAX_TIMEOUT);

        new Thread(() -> {
            //Messages messagesClient = Clients.MessagesClient.get(destDomain, this.discovery);
            long startTime = System.currentTimeMillis();

            while (!success.get() && !timeIsUp.get() && (System.currentTimeMillis()-startTime)<MAX_TIMEOUT){
                try {
                    Messages messagesClient = Clients.MessagesClient.get(destDomain, this.discovery);

                    Result<String> res = messagesClient.postMessage(pwd, msg);
                    if (res.isOK()) {
                        success.set(true);
                        timer.cancel();
                    }else{
                        if(!timeIsUp.get()){
                            try { Thread.sleep(RETRY_DELAY); } catch (InterruptedException ignored) {}
                        }
                    }
                } catch (Exception e) {
                    if(!timeIsUp.get()){
                        try { Thread.sleep(RETRY_DELAY); } catch (InterruptedException ignored) {}
                    }
                }
            }
        }).start();

    }


        /*new Thread(()->{
            long startTime = System.currentTimeMillis();
            boolean success = false;
            Messages messagesClient = Clients.MessagesClient.get(destDomain, this.discovery);
            while(!success &&(System.currentTimeMillis()-startTime)<MAX_TIMEOUT) {
                try{
                    messagesClient.postMessage(pwd, msg);
                    success = true;
                }catch(Exception e){
                    try { Thread.sleep(RETRY_DELAY); } catch (InterruptedException ignored) {}
                }
            }
            if(!success){
                Log.warning("Gave up sending to " + destDomain + " after 90s.");
                String destUser = "";
                for(String d : msg.getDestination()) {
                    if(d.endsWith(destDomain)){
                        destUser = d;
                        break;
                    }
                }
                if(!destUser.isEmpty()){
                    notificationDelivery(msg, destUser, TIMEOUT);
                }
            }

        }).start();
    }*/

    /*private void generateTimeoutNotification(String destDomain, Message msg) {
        Log.warning("Gave up sending to " + destDomain + " after 90s.");
        String destUser = "";
        for(String d : msg.getDestination()) {
            if(d.endsWith(destDomain)){
                destUser = d;
                break;
            }
        }
        if(!destUser.isEmpty()){
            notificationDelivery(msg, destUser, TIMEOUT);
        }
    }*/

    private Message fetchMessage(String mid) {
        String msgQuery = String.format(SELECT_MESSAGE, mid);
        List<Message> messages = hibernate.jpql(msgQuery, Message.class);
        if(messages.isEmpty())
            return null;
        return messages.get(0);
    }
    private void executeDelete(Message msg) {
        //apagar inboxes
        String ibxQuery = String.format(SELECT_INBOX_BY_MID, msg.getId());
        List<Inbox> inboxes = hibernate.jpql(ibxQuery, Inbox.class);
        for (Inbox i : inboxes) {
            try {
                hibernate.delete(i);
            } catch (Exception ignored) {}
        }
        //apagar mensagens
        try {
            hibernate.delete(msg);
        } catch (Exception ignored) {}
    }
    private void remoteDelete(Set<String> destination, String mid) {
        Set<String> remoteDomains = new HashSet<>();

        for(String dest : destination){
            String destDomain = extractDomain(dest);
            if (!destDomain.equals(this.domain)) {
                remoteDomains.add(destDomain);
            }
        }
        for(String remoteDomain : remoteDomains){
            new Thread(()->{
                long startTime = System.currentTimeMillis();
                boolean success = false;
                Messages messagesClient = Clients.MessagesClient.get(remoteDomain, this.discovery);
                while(!success &&(System.currentTimeMillis()-startTime)<MAX_TIMEOUT) {
                    try{
                        messagesClient.internalDeleteMessage(mid);
                        success = true;
                    }catch(Exception e){
                        try { Thread.sleep(RETRY_DELAY); } catch (InterruptedException ignored) {}
                    }
                }
            }).start();
        }
    }

    /*private interface RemoteAction {
        void execute() throws Exception;
    }
    private void executeWithRetry(String destDomain, RemoteAction action, Runnable onTimeout) {
        Timer timer = new Timer(true);
        AtomicBoolean success = new AtomicBoolean(false);
        AtomicBoolean timeIsUp = new AtomicBoolean(false);

        timer.schedule(new TimerTask(){
            @Override
            public void run() {
                timeIsUp.set(true);
                if(!success.get() && onTimeout != null){
                    onTimeout.run();
                }
            }
        }, MAX_TIMEOUT);

        new Thread(() -> {
            while (!success.get() && !timeIsUp.get()){
                try {
                    action.execute();
                    success.set(true);
                    timer.cancel();
                } catch (Exception e) {
                    if(!timeIsUp.get()){
                        try { Thread.sleep(RETRY_DELAY); } catch (InterruptedException ignored) {}
                    }
                }
            }
        }).start();

    }*/

}