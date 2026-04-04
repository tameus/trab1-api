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

public class JavaMessages implements Messages {

    private static Logger Log = Logger.getLogger(JavaMessages.class.getName());

    private final String domain;
    private final Discovery discovery;
    private final Hibernate hibernate;

    private final MessagesAux messAux;

    public JavaMessages(String domain, Discovery discovery) {
        this.domain = domain;
        this.discovery = discovery;
        this.hibernate = Hibernate.getInstance(); // A BD já está aqui pronta a usar!
        this.messAux = new MessagesAux(domain,discovery,hibernate);

    }


    @Override
    public Result<String> postMessage(String pwd, Message msg) {
        Log.info("postMessage processando...");

        // TODO 1: Validar se pwd ou msg são nulos (Retornar Result.ErrorCode.BAD_REQUEST)

        if(msg == null||pwd == null||msg.getSender()==null||msg.getDestination()==null||!msg.getSender().endsWith("@" + this.domain)){
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }
        // TODO 2: Validar as credenciais do Sender!
        var user = messAux.checkUser(msg.getSender(),pwd,this.domain);
        if(!user.isOK()){
            return Result.error(user.error());
        }
        User senderUser = user.value();


        // TODO 3: Gerar um ID único para a mensagem (Podes usar java.util.UUID.randomUUID().toString())
        String mid = msg.getId();
        if(mid == null||mid.isBlank()){
            mid = java.util.UUID.randomUUID().toString();
        }

        String fullMid = null;
        if (mid.contains("@")) {
            fullMid = mid;
        }else{
            fullMid = mid + "@" + this.domain;
        }

        msg.setId(fullMid);
       // if(hibernate.get(Message.class,fullMid)!=null){
         //   return Result.ok(fullMid);
        //}

        // TODO 4: Transformar o campo 'sender' da mensagem no formato "DisplayName <nome@dominio>".
        // (Vais ter de ir buscar o DisplayName ao utilizador que validaste no passo 2).
            msg.setSender(String.format("%s <%s@%s>",
                    senderUser.getDisplayName(),
                    senderUser.getName(),
                    senderUser.getDomain()
                    ));

        msg.setCreationTime(System.currentTimeMillis());

        try{
            if(msg.getSender()!=null && msg.getContents().length()> 255){
                msg.setContents(msg.getContents().substring(0,251)+"...");
            }

            if (msg.getSubject() != null && msg.getSubject().length() > 255) {
                msg.setSubject(msg.getSubject().substring(0, 255));
            }

            Message existingMsg = hibernate.get(Message.class,fullMid);
            if(existingMsg == null){
                hibernate.persist(msg);
            }

            // TODO 5: Gravar a mensagem na base de dados ( hibernate.persist(msg) )

            // TODO 6: Para cada destinatário no msg.getDestination(), adicionar esta mensagem à Caixa de Entrada (Inbox) deles na BD.
            for(String dest:msg.getDestination()){
                String userPart = dest.contains("@") ? dest.split("@")[0] : dest;
                String domainPart = dest.contains("@") ? dest.split("@")[1] : this.domain;
                hibernate.persist(new Inbox(userPart, fullMid));

                if(domainPart.equals(this.domain)){
                var res = messAux.checkUser(userPart,null,this.domain);
                if(res.isOK()){
                    hibernate.persist(new Inbox(userPart, fullMid));
                }else if(res.error() == Result.ErrorCode.NOT_FOUND){
                    messAux.failureNoti(msg.getSender(),msg,dest);
                }

            }else{
                    // TODO 7: Mais tarde (para o teste 8a), reencaminhar a mensagem se o destinatário for de outro domínio.

                }
            }
            return Result.ok(fullMid);
        }catch(Exception e){
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }

    }





    @Override
    public Result<Message> getInboxMessage(String name, String mid, String pwd) {
        // TODO 1: Validar credenciais do utilizador 'name' no servidor de Users (FORBIDDEN se falhar)
        var user = messAux.checkUser(name,pwd,this.domain);
        if(!user.isOK()){
            return Result.error(user.error());
        }

        // TODO 2: Ir à base de dados verificar se o ID 'mid' existe na INBOX deste utilizador.
        try{

            String query = String.format("SELECT i FROM Inbox i WHERE i.userAddress = '%s' AND i.messageId = '%s'",name,mid);
            var results = hibernate.jpql(query, Inbox.class);
            if(results.isEmpty()){
                return Result.error(Result.ErrorCode.NOT_FOUND);
            }

            String msgQuery = String.format("SELECT m FROM Message m LEFT JOIN FETCH m.destination WHERE m.id = '%s'", mid);
            var messages = hibernate.jpql(msgQuery, Message.class);
            if(messages.isEmpty()) return Result.error(Result.ErrorCode.NOT_FOUND);
            return Result.ok(messages.get(0));

            //Message msg = hibernate.get(Message.class,mid);
            //if(msg == null){
              //  return Result.error(Result.ErrorCode.NOT_FOUND);
            //}

            //return Result.ok(msg);
        }catch(Exception e){
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);

        }
        // TODO 3: Se existir, ir buscar a Message à BD e retornar Result.ok(msg). Se não, NOT_FOUND.
    }

    @Override
    public Result<List<String>> getAllInboxMessages(String name, String pwd) {
        // TODO 1: Validar credenciais do utilizador (FORBIDDEN se falhar)
        var user = messAux.checkUser(name,pwd,this.domain);
        if(!user.isOK())return Result.error(user.error());
        // TODO 2: Fazer uma query à BD (hibernate.jpql) para buscar todos os IDs de mensagens na INBOX deste 'name'.
        try{
            String query = String.format("SELECT i.messageId FROM Inbox i WHERE i.userAddress = '%s'", name);

            List<String> msgIds = hibernate.jpql(query,String.class);


            return Result.ok(msgIds);

        }catch(Exception e){
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);

        }
    }

    @Override
    public Result<Void> removeInboxMessage(String name, String mid, String pwd) {
        // TODO 1: Validar credenciais (FORBIDDEN)

        var user = messAux.checkUser(name,pwd,this.domain);
        if(!user.isOK()) return Result.error(user.error());

        // TODO 2: Apagar a entrada correspondente a este 'mid' e 'name' na tabela INBOX.
        // Atenção: NÃO apagar a mensagem da tabela Message! Apenas apagar da Inbox do utilizador.

        try{
            String query = String.format("SELECT i FROM Inbox i WHERE i.userAddress = '%s' AND i.messageId = '%s'",name,mid);

            List<Inbox> entries = hibernate.jpql(query,Inbox.class);

            if(entries.isEmpty()){
                return Result.error(Result.ErrorCode.NOT_FOUND);
            }
            hibernate.delete(entries.get(0));

            return Result.ok();

        }catch(Exception e){
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);

        }

    }

    @Override
    public Result<Void> deleteMessage(String name, String mid, String pwd) {
        // TODO 1: Validar credenciais do 'name' (Sender) (FORBIDDEN)

        var user = messAux.checkUser(name,pwd,this.domain);
        if(!user.isOK()) return Result.error(user.error());
        // TODO 2: Ir buscar a mensagem 'mid' à BD. Se o 'name' não for o sender original, erro!
        try {
            Message msg = hibernate.get(Message.class,mid);
            if(msg == null) return Result.error(Result.ErrorCode.NOT_FOUND);

            String expected = String.format("<%s@%s>",name,this.domain);
            if(!msg.getSender().contains(expected)) return Result.error(Result.ErrorCode.FORBIDDEN);
            // TODO 3: Se a mensagem tiver sido criada há mais de 30 segundos, dar erro.
            long now = System.currentTimeMillis();
            // esta em milisegundos
            if(now - msg.getCreationTime()> 30000){
                return Result.error(Result.ErrorCode.FORBIDDEN);
            }

            // TODO 4: Se tudo estiver OK, apagar a mensagem da tabela Message e de TODAS as Inboxes.
            String query = String.format("SELECT i FROM Inbox i WHERE i.messageId = '%s'", mid);
            List<Inbox>inboxes = hibernate.jpql(query,Inbox.class);

            for(Inbox i : inboxes){
                hibernate.delete(i);
            }
            hibernate.delete(msg);

            //TODO 8a

            return Result.ok();


        } catch (Exception e) {
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);

        }
    }

    @Override
    public Result<List<String>> searchInbox(String name, String pwd, String query) {
        // TODO 1: Validar credenciais (FORBIDDEN)
        // TODO 2: Buscar as mensagens da Inbox do user onde o subject OU o contents contenham a 'query' (case-insensitive).
        return Result.error(Result.ErrorCode.NOT_IMPLEMENTED);
    }
}