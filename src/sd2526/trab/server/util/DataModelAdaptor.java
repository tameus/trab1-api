package sd2526.trab.server.util;

import java.util.HashSet;

import sd2526.trab.api.Message;
import sd2526.trab.api.User;
import sd2526.trab.api.grpc.Users.GrpcUser;
import sd2526.trab.api.grpc.Messages.GrpcMessage;


public class DataModelAdaptor {

    public static User GrpcUser_to_User(GrpcUser from) {
        return new User(
                from.getName().isEmpty() ? null : from.getName(),
                from.hasPwd() ? from.getPwd() : null,
                from.hasDisplayName() ? from.getDisplayName() : null,
                from.hasDomain() ? from.getDomain() : null
        );
    }

    public static GrpcUser User_to_GrpcUser(User from) {
        GrpcUser.Builder b = GrpcUser.newBuilder();

        if (from.getName() != null)
            b.setName(from.getName());

        if (from.getPwd() != null)
            b.setPwd(from.getPwd());

        if (from.getDisplayName() != null)
            b.setDisplayName(from.getDisplayName());

        if (from.getDomain() != null)
            b.setDomain(from.getDomain());

        return b.build();
    }

    public static Message GrpcMessage_to_Message(GrpcMessage from) {
        Message m = new Message();

        m.setId(from.getId().isEmpty() ? null : from.getId());
        m.setSender(from.getSender().isEmpty() ? null : from.getSender());
        m.setSubject(from.getSubject().isEmpty() ? null : from.getSubject());
        m.setContents(from.getContents().isEmpty() ? null : from.getContents());

        if (from.getDestinationList() != null) {
            m.setDestination(new HashSet<>(from.getDestinationList()));
        }

        m.setCreationTime(from.getCreationTime());

        return m;
    }

    public static GrpcMessage Message_to_GrpcMessage(Message from) {
        GrpcMessage.Builder b = GrpcMessage.newBuilder();

        if (from.getId() != null)
            b.setId(from.getId());

        if (from.getSender() != null)
            b.setSender(from.getSender());

        if (from.getSubject() != null)
            b.setSubject(from.getSubject());

        if (from.getContents() != null)
            b.setContents(from.getContents());

        if (from.getDestination() != null)
            b.addAllDestination(from.getDestination());

        b.setCreationTime(from.getCreationTime());

        return b.build();
    }
}
