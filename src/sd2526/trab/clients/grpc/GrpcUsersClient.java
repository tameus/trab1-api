package sd2526.trab.clients.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import sd2526.trab.api.User;
import sd2526.trab.api.java.Result;

import sd2526.trab.api.java.Users;
import sd2526.trab.api.java.Users.*;
import sd2526.trab.api.grpc.GrpcUsersGrpc;
import sd2526.trab.api.grpc.Users.GrpcUser;
import sd2526.trab.api.grpc.Users.PostUserResult;
import sd2526.trab.api.grpc.Users.GetUserArgs;
import sd2526.trab.api.grpc.Users.GetUserResult;
import sd2526.trab.api.grpc.Users.UpdateUserArgs;
import sd2526.trab.api.grpc.Users.UpdateUserResult;
import sd2526.trab.api.grpc.Users.DeleteUserArgs;
import sd2526.trab.api.grpc.Users.DeleteUserResult;
import sd2526.trab.api.grpc.Users.SearchUsersArgs;
import sd2526.trab.api.grpc.GrpcUsersGrpc;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GrpcUsersClient implements Users {

    private final ManagedChannel channel;
    private final GrpcUsersGrpc.GrpcUsersBlockingStub stub;

    public GrpcUsersClient(URI serverUri){
        this.channel = ManagedChannelBuilder.forAddress(serverUri.getHost(), serverUri.getPort())
                .usePlaintext()
                .build();
        this.stub = GrpcUsersGrpc.newBlockingStub(channel);

    }
    @Override
    public Result<String> postUser(User user) {
        try {
            GrpcUser userser = GrpcUser.newBuilder()
                    .setName(user.getName())
                    .setPwd(user.getPwd())
                    .setDisplayName(user.getDisplayName())
                    .setDomain(user.getDomain())
                    .build();
            PostUserResult res = stub.postUser(userser);
            return Result.ok(res.getUserAddress());
        } catch (StatusRuntimeException e) {
           return Result.error(mapError(e.getStatus()));
        }
    }

    @Override
    public Result<User> getUser(String name, String pwd) {
        try {
            GetUserArgs args = GetUserArgs.newBuilder()
                    .setName(name)
                    .setPwd(pwd)
                    .build();

            GetUserResult res = stub.getUser(args);
            GrpcUser user = res.getUser();
            return Result.ok(new User(user.getName(),pwd,user.getDisplayName(),user.getDomain()));
        } catch (StatusRuntimeException e) {
            return Result.error(mapError(e.getStatus()));
        }

    }

    @Override
    public Result<User> updateUser(String name, String pwd, User info) {
        try {
            GrpcUser gInfo = GrpcUser.newBuilder()
                    .setName(info.getName())
                    .setPwd(info.getPwd())
                    .setDisplayName(info.getDisplayName())
                    .setDomain(info.getDomain())
                    .build();

            UpdateUserArgs args = UpdateUserArgs.newBuilder()
                    .setName(name)
                    .setPwd(pwd)
                    .setInfo(gInfo)
                    .build();

            UpdateUserResult res = stub.updateUser(args);
            GrpcUser user = res.getUser();
            return Result.ok(new User(user.getName(), user.getPwd(), user.getDisplayName(), user.getDomain()));
        } catch (StatusRuntimeException e) {
            return Result.error(mapError(e.getStatus()));
        }
    }

    @Override
    public Result<User> deleteUser(String name, String pwd) {
        try {
            DeleteUserArgs args = DeleteUserArgs.newBuilder()
                    .setName(name)
                    .setPwd(pwd)
                    .build();

            DeleteUserResult res = stub.deleteUser(args);
            GrpcUser user = res.getUser();
            return Result.ok(new User(user.getName(), user.getPwd(), user.getDisplayName(), user.getDomain()));
        } catch (StatusRuntimeException e) {
            return Result.error(mapError(e.getStatus()));
        }
    }

    @Override
    public Result<List<User>> searchUsers(String name, String pwd, String query) {
        try {
            SearchUsersArgs args = SearchUsersArgs.newBuilder()
                    .setName(name)
                    .setPwd(pwd)
                    .setQuery(query)
                    .build();

            Iterator<GrpcUser> users = stub.searchUsers(args);
            List<User> results = new ArrayList<>();
            while (users.hasNext()) {
                GrpcUser user = users.next();
                results.add(new User(user.getName(), "", user.getDisplayName(), user.getDomain()));
            }
            return Result.ok(results);
        } catch (StatusRuntimeException e) {
            return Result.error(mapError(e.getStatus()));
        }
    }

    @Override
    public Result<User> verifyUser(String name) {
        return getUser(name, "");
    }

    private Result.ErrorCode mapError(io.grpc.Status status){
        return switch (status.getCode()) {
            case NOT_FOUND -> Result.ErrorCode.NOT_FOUND;
            case PERMISSION_DENIED -> Result.ErrorCode.FORBIDDEN;
            case INVALID_ARGUMENT -> Result.ErrorCode.BAD_REQUEST;
            case ALREADY_EXISTS -> Result.ErrorCode.CONFLICT;
            case INTERNAL -> Result.ErrorCode.INTERNAL_ERROR;
            case UNIMPLEMENTED -> Result.ErrorCode.NOT_IMPLEMENTED;
            default -> Result.ErrorCode.INTERNAL_ERROR;
        };
    }
}
