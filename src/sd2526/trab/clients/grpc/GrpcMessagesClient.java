package sd2526.trab.clients.grpc;

import sd2526.trab.api.Message;
import sd2526.trab.api.grpc.GrpcMessagesGrpc;
import sd2526.trab.api.java.Messages;
import sd2526.trab.api.java.Result;

import java.net.URI;
import java.util.List;
import java.util.logging.Logger;
import sd2526.trab.api.grpc.Messages.PostMessageArgs;
import sd2526.trab.server.util.DataModelAdaptor;
import sd2526.trab.api.grpc.Messages.GetInboxMessageArgs;
import sd2526.trab.api.grpc.Messages.GetAllInboxMessagesArgs;
import sd2526.trab.api.grpc.Messages.RemoveInboxMessageArgs;
import sd2526.trab.api.grpc.Messages.DeleteMessageArgs;
import sd2526.trab.api.grpc.Messages.SearchInboxArgs;
import sd2526.trab.api.grpc.Messages.InternalDeleteMessageArgs;



public class GrpcMessagesClient extends GrpcClient implements Messages {

    private static Logger Log = Logger.getLogger(GrpcMessagesClient.class.getName());
    final GrpcMessagesGrpc.GrpcMessagesBlockingStub stub;

    public GrpcMessagesClient(URI serverURI) {
        super(serverURI, Log);
        stub = GrpcMessagesGrpc.newBlockingStub(channel);
    }

    @Override
    public Result<String> postMessage(String pwd, Message msg) {
        return super.processResponse(() -> {
            var safePwd = (pwd == null) ? "" : pwd;
            var args = PostMessageArgs.newBuilder().setPwd(safePwd).setMessage(DataModelAdaptor.Message_to_GrpcMessage(msg)).build();
            var res = stub.postMessage(args);
            return res.getMid();
        });
    }

    @Override
    public Result<Message> getInboxMessage(String name, String mid, String pwd) {
        return super.processResponse(() -> {
            var safePwd = (pwd == null) ? "" : pwd;
            var args = GetInboxMessageArgs.newBuilder().setName(name).setMid(mid).setPwd(safePwd).build();
            var res = stub.getInboxMessage(args);
            return DataModelAdaptor.GrpcMessage_to_Message(res);
        });
    }

    @Override
    public Result<List<String>> getAllInboxMessages(String name, String pwd) {
        return super.processResponse(() -> {
            var safePwd = (pwd == null) ? "" : pwd;
            var args = GetAllInboxMessagesArgs.newBuilder().setName(name).setPwd(safePwd).build();
            var res = stub.getAllInboxMessages(args);
            return res.getMidsList();
        });
    }

    @Override
    public Result<Void> removeInboxMessage(String name, String mid, String pwd) {
        return super.processResponse(() -> {
            var safePwd = (pwd == null) ? "" : pwd;
            var args = RemoveInboxMessageArgs.newBuilder().setName(name).setMid(mid).setPwd(safePwd).build();
            stub.removeInboxMessage(args);
        });
    }

    @Override
    public Result<Void> deleteMessage(String name, String mid, String pwd) {
        return super.processResponse(() -> {
            var safePwd = (pwd == null) ? "" : pwd;
            var args = DeleteMessageArgs.newBuilder().setName(name).setMid(mid).setPwd(safePwd).build();
            stub.deleteMessage(args);
        });
    }

    @Override
    public Result<List<String>> searchInbox(String name, String pwd, String query) {
        return super.processResponse(() -> {
            var safePwd = (pwd == null) ? "" : pwd;
            var safeQuery = (query == null) ? "" : query;
            var args = SearchInboxArgs.newBuilder().setName(name).setPwd(safePwd).setQuery(safeQuery).build();
            var res = stub.searchInbox(args);
            return res.getMidsList();
        });
    }

    @Override
    public Result<String> internalDeleteMessage(String mid) {
        return super.processResponse(() -> {
            var args = InternalDeleteMessageArgs.newBuilder().setMid(mid).build();
            stub.internalDeleteMessage(args);
            return mid;
        });
    }
}