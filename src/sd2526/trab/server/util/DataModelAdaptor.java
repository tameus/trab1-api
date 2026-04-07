package sd2526.trab.server.util;

import java.util.HashSet;

import com.google.protobuf.ByteString;

import sd2526.trab.api.User;
import sd2526.trab.api.grpc.Users.GrpcUser;
import sd2526.trab.api.grpc.Users.GrpcUser.Builder;

public class DataModelAdaptor {

    public static User GrpcUser_to_User( GrpcUser from )  {
        return new User(
                from.getName(),
                from.hasPwd() ? from.getPwd() : null,
                from.hasDisplayName() ? from.getDisplayName() : null,
                from.hasDomain() ? from.getDomain() : null,
                from.hasPhoto() ? from.getPhoto().toByteArray() : null,
                new HashSet<>(from.getPhoneNumbersList()));
    }

    public static GrpcUser User_to_GrpcUser( User from )  {
        Builder b = GrpcUser.newBuilder();

        if(from.getName() != null)
            b.setName( from.getName());

        if(from.getPwd() != null)
            b.setPwd( from.getPwd());

        if(from.getDisplayName() != null)
            b.setDisplayName( from.getDisplayName());

        if(from.getDomain() != null)
            b.setDomain( from.getDomain());

        if(from.getPhoto() != null)
            b.setPhoto( ByteString.copyFrom(from.getPhoto()));

        if(from.getPhoneNumbers() != null)
            b.addAllPhoneNumbers( from.getPhoneNumbers());

        return b.build();
    }

}
