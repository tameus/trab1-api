package sd2526.trab.server.java;

import sd2526.trab.Discovery;
import sd2526.trab.api.Message;
import sd2526.trab.api.java.Messages;
import sd2526.trab.api.java.Result;

import java.util.List;

public class JavaMessages implements Messages {
    public JavaMessages(String domain, Discovery discovery) {

    }

    @Override
    public Result<String> postMessage(String pwd, Message msg) {
        return Result.error(Result.ErrorCode.NOT_IMPLEMENTED);
    }

    @Override
    public Result<Message> getInboxMessage(String name, String mid, String pwd) {
        return Result.error(Result.ErrorCode.NOT_IMPLEMENTED);
    }

    @Override
    public Result<List<String>> getAllInboxMessages(String name, String pwd) {
        return Result.error(Result.ErrorCode.NOT_IMPLEMENTED);
    }

    @Override
    public Result<Void> removeInboxMessage(String name, String mid, String pwd) {
        return Result.error(Result.ErrorCode.NOT_IMPLEMENTED);
    }

    @Override
    public Result<Void> deleteMessage(String name, String mid, String pwd) {
        return Result.error(Result.ErrorCode.NOT_IMPLEMENTED);
    }

    @Override
    public Result<List<String>> searchInbox(String name, String pwd, String query) {
        return Result.error(Result.ErrorCode.NOT_IMPLEMENTED);
    }
}
