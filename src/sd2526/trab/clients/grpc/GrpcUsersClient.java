package sd2526.trab.clients.grpc;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import sd2526.trab.api.User;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Users;
import sd2526.trab.api.grpc.GrpcUsersGrpc;
import sd2526.trab.api.grpc.Users.GetUserArgs;
import sd2526.trab.server.util.DataModelAdaptor;
import sd2526.trab.api.grpc.Users.UpdateUserArgs;
import sd2526.trab.api.grpc.Users.DeleteUserArgs;
import sd2526.trab.api.grpc.Users.SearchUsersArgs;

public class GrpcUsersClient extends GrpcClient implements Users {

    private static Logger Log = Logger.getLogger(GrpcUsersClient.class.getName());
    final GrpcUsersGrpc.GrpcUsersBlockingStub stub;

    public GrpcUsersClient(URI serverURI) {
        super( serverURI, Log );
        stub = GrpcUsersGrpc.newBlockingStub( channel );
    }

    @Override
    public Result<String> postUser(User user) {
        return super.processResponse(() -> {
            var grpcUser = DataModelAdaptor.User_to_GrpcUser(user);
            var res = stub.postUser(grpcUser);
            return res.getUserAddress();
        });
    }

    @Override
    public Result<User> getUser(String userId, String password) {
        return super.processResponse(() -> {
            var safeUserId = (userId == null) ? "" : userId;
            var safePwd = (password == null) ? "" : password;

            var args = GetUserArgs.newBuilder().setName(safeUserId).setPwd(safePwd).build();
            var res = stub.getUser(args);
            return DataModelAdaptor.GrpcUser_to_User(res.getUser());
        });
    }

    @Override
    public Result<User> updateUser(String userId, String pwd, User user) {
        return super.processResponse(() -> {
            var safeUserId = (userId == null) ? "" : userId;
            var safePwd = (pwd == null) ? "" : pwd;

            var grpcInfo = DataModelAdaptor.User_to_GrpcUser(user);
            var args = UpdateUserArgs.newBuilder().setName(safeUserId).setPwd(safePwd).setInfo(grpcInfo).build();
            var res = stub.updateUser(args);
            return DataModelAdaptor.GrpcUser_to_User(res.getUser());
        });
    }

    @Override
    public Result<User> deleteUser(String userId, String pwd) {
        return super.processResponse(() -> {
            var safeUserId = (userId == null) ? "" : userId;
            var safePwd = (pwd == null) ? "" : pwd;

            var args = DeleteUserArgs.newBuilder().setName(safeUserId).setPwd(safePwd).build();
            var res = stub.deleteUser(args);
            return DataModelAdaptor.GrpcUser_to_User(res.getUser());
        });
    }

    @Override
    public Result<List<User>> searchUsers(String name, String pwd, String query) {
        return super.processResponse(() -> {
            var safeName = (name == null) ? "" : name;
            var safePwd = (pwd == null) ? "" : pwd;
            var safeQuery = (query == null) ? "" : query;

            var args = SearchUsersArgs.newBuilder().setName(safeName).setPwd(safePwd).setQuery(safeQuery).build();
            var iterator = stub.searchUsers(args);

            List<User> list = new ArrayList<>();
            while (iterator.hasNext()) {
                list.add(DataModelAdaptor.GrpcUser_to_User(iterator.next()));
            }
            return list;
        });
    }

    @Override
    public Result<User> verifyUser(String name) {
        return super.processResponse(() -> {
            var safeName = (name == null) ? "" : name;
            var args = sd2526.trab.api.grpc.Users.VerifyUserArgs.newBuilder().setName(safeName).build();
            var res = stub.verifyUser(args);
            return DataModelAdaptor.GrpcUser_to_User(res);
        });
    }
}