package sd2526.trab.server.java;

import sd2526.trab.Discovery;
import sd2526.trab.api.Message;
import sd2526.trab.api.java.Messages;
import sd2526.trab.api.java.Result;
import sd2526.trab.server.persistence.Hibernate;

import java.util.List;
import java.util.logging.Logger;

public class JavaMessages implements Messages {

    private static Logger Log = Logger.getLogger(JavaMessages.class.getName());

    private final String domain;
    private final Discovery discovery;
    private final Hibernate hibernate;

    public JavaMessages(String domain, Discovery discovery) {
        this.domain = domain;
        this.discovery = discovery;
        this.hibernate = Hibernate.getInstance(); // A BD já está aqui pronta a usar!
    }

    @Override
    public Result<String> postMessage(String pwd, Message msg) {
        Log.info("postMessage processando...");

        // TODO 1: Validar se pwd ou msg são nulos (Retornar Result.ErrorCode.BAD_REQUEST)

        // TODO 2: Validar as credenciais do Sender!
        // Como? O sender tem o formato "nome@dominio" (ex: "sara@fct").
        // Tens de fazer um pedido REST ao servidor de Users local para verificar se a pwd está certa.
        // Se falhar, retornar Result.ErrorCode.FORBIDDEN.

        // TODO 3: Gerar um ID único para a mensagem (Podes usar java.util.UUID.randomUUID().toString())

        // TODO 4: Transformar o campo 'sender' da mensagem no formato "DisplayName <nome@dominio>".
        // (Vais ter de ir buscar o DisplayName ao utilizador que validaste no passo 2).

        // TODO 5: Gravar a mensagem na base de dados ( hibernate.persist(msg) )

        // TODO 6: Para cada destinatário no msg.getDestination(), adicionar esta mensagem à Caixa de Entrada (Inbox) deles na BD.

        // TODO 7: Mais tarde (para o teste 8a), reencaminhar a mensagem se o destinatário for de outro domínio.

        return Result.error(Result.ErrorCode.NOT_IMPLEMENTED);
    }

    @Override
    public Result<Message> getInboxMessage(String name, String mid, String pwd) {
        // TODO 1: Validar credenciais do utilizador 'name' no servidor de Users (FORBIDDEN se falhar)
        // TODO 2: Ir à base de dados verificar se o ID 'mid' existe na INBOX deste utilizador.
        // TODO 3: Se existir, ir buscar a Message à BD e retornar Result.ok(msg). Se não, NOT_FOUND.
        return Result.error(Result.ErrorCode.NOT_IMPLEMENTED);
    }

    @Override
    public Result<List<String>> getAllInboxMessages(String name, String pwd) {
        // TODO 1: Validar credenciais do utilizador (FORBIDDEN se falhar)
        // TODO 2: Fazer uma query à BD (hibernate.jpql) para buscar todos os IDs de mensagens na INBOX deste 'name'.
        return Result.error(Result.ErrorCode.NOT_IMPLEMENTED);
    }

    @Override
    public Result<Void> removeInboxMessage(String name, String mid, String pwd) {
        // TODO 1: Validar credenciais (FORBIDDEN)
        // TODO 2: Apagar a entrada correspondente a este 'mid' e 'name' na tabela INBOX.
        // Atenção: NÃO apagar a mensagem da tabela Message! Apenas apagar da Inbox do utilizador.
        return Result.error(Result.ErrorCode.NOT_IMPLEMENTED);
    }

    @Override
    public Result<Void> deleteMessage(String name, String mid, String pwd) {
        // TODO 1: Validar credenciais do 'name' (Sender) (FORBIDDEN)
        // TODO 2: Ir buscar a mensagem 'mid' à BD. Se o 'name' não for o sender original, erro!
        // TODO 3: Se a mensagem tiver sido criada há mais de 30 segundos, dar erro.
        // TODO 4: Se tudo estiver OK, apagar a mensagem da tabela Message e de TODAS as Inboxes.
        return Result.error(Result.ErrorCode.NOT_IMPLEMENTED);
    }

    @Override
    public Result<List<String>> searchInbox(String name, String pwd, String query) {
        // TODO 1: Validar credenciais (FORBIDDEN)
        // TODO 2: Buscar as mensagens da Inbox do user onde o subject OU o contents contenham a 'query' (case-insensitive).
        return Result.error(Result.ErrorCode.NOT_IMPLEMENTED);
    }
}