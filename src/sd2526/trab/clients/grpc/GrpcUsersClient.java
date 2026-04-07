package sd2526.trab.clients.grpc;

import java.net.URI;
import java.util.List;
import java.util.logging.Logger;

import sd2526.trab.api.User;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Result.ErrorCode;
import sd2526.trab.api.java.Users;
import sd2526.trab.api.grpc.GrpcUsersGrpc;
import sd2526.trab.api.grpc.Users.GetUserArgs;
import sd2526.trab.api.grpc.Users.GetUserResult;
import sd2526.trab.server.util.DataModelAdaptor;

public class GrpcUsersClient extends GrpcClient implements Users {

    private static Logger Log = Logger.getLogger(GrpcUsersClient.class.getName());


    final GrpcUsersGrpc.GrpcUsersBlockingStub stub;

    public GrpcUsersClient(URI serverURI) {
        super( serverURI, Log );

        stub = GrpcUsersGrpc.newBlockingStub( channel );
    }

    @Override
    public Result<String> postUser(User user) {
        return super.processResponse( () -> stub.postUser(DataModelAdaptor.User_to_GrpcUser(user)).getUserAddress() );
    }

    @Override
    public Result<User> getUser(String userId, String password) {
        GetUserResult res = stub.getUser(GetUserArgs.newBuilder()
                .setName(userId).setPwd(password)
                .build());

        return super.processResponse( () -> DataModelAdaptor.GrpcUser_to_User(res.getUser()) );
    }

    @Override
    public Result<User> updateUser(String userId, String pwd, User user) {
        throw new RuntimeException(ErrorCode.NOT_IMPLEMENTED.toString());
    }

    @Override
    public Result<User> deleteUser(String userId, String pwd) {
        throw new RuntimeException(ErrorCode.NOT_IMPLEMENTED.toString());
    }

    @Override
    public Result<List<User>> searchUsers(String name, String pwd, String query) {
        return null;
    }

    @Override
    public Result<User> verifyUser(String name) {
        return null;
    }

    @Override
    public Result<List<User>> searchUsers(String pattern) {
        throw new RuntimeException(ErrorCode.NOT_IMPLEMENTED.toString());
    }

    @Override
    public Result<byte[]> getUserPhoto(String name, String pwd) {
        throw new RuntimeException(ErrorCode.NOT_IMPLEMENTED.toString());
    }

    @Override
    public Result<User> updateUserPhoto(String name, String pwd, byte[] photo) {
        throw new RuntimeException(ErrorCode.NOT_IMPLEMENTED.toString());
    }


}
