package sd2526.trab.server.grpc;

import static sd2526.trab.server.util.DataModelAdaptor.GrpcUser_to_User;
import static sd2526.trab.server.util.DataModelAdaptor.User_to_GrpcUser;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;
import sd2526.trab.api.java.Result.ErrorCode;
import sd2526.trab.api.java.Users;
import sd2526.trab.api.grpc.GrpcUsersGrpc;
import sd2526.trab.api.grpc.Users.DeleteUserArgs;
import sd2526.trab.api.grpc.Users.DeleteUserResult;
import sd2526.trab.api.grpc.Users.GetUserArgs;
import sd2526.trab.api.grpc.Users.GetUserResult;
import sd2526.trab.api.grpc.Users.GrpcUser;
import sd2526.trab.api.grpc.Users.PostUserResult;
import sd2526.trab.api.grpc.Users.SearchUsersArgs;
import sd2526.trab.api.grpc.Users.UpdateUserArgs;
import sd2526.trab.api.grpc.Users.UpdateUserResult;
import sd2526.trab.api.grpc.Users.VerifyUserArgs;
import sd2526.trab.server.java.JavaUsers;


public class GrpcUsersController extends GrpcController implements GrpcUsersGrpc.AsyncService, BindableService {

    final Users impl;

    public GrpcUsersController(String domain) {
        this.impl = new JavaUsers(domain);
    }

    @Override
    public final ServerServiceDefinition bindService() {
        return GrpcUsersGrpc.bindService(this);
    }

    public void postUser(GrpcUser user, StreamObserver<PostUserResult> responseObserver) {
        super.toGrpcResult(responseObserver, impl.postUser(GrpcUser_to_User(user)),
                (userAddress) -> PostUserResult.newBuilder().setUserAddress(userAddress).build());
    }

    @Override
    public void getUser(GetUserArgs request, StreamObserver<GetUserResult> responseObserver) {
        super.toGrpcResult(responseObserver, impl.getUser(request.getName(), request.getPwd()),
                (user) -> GetUserResult.newBuilder().setUser(User_to_GrpcUser(user)).build());
    }

    @Override
    public void updateUser(UpdateUserArgs request, StreamObserver<UpdateUserResult> responseObserver) {
        super.toGrpcResult(responseObserver, impl.updateUser(request.getName(), request.getPwd(), GrpcUser_to_User(request.getInfo())),
                (user) -> UpdateUserResult.newBuilder().setUser(User_to_GrpcUser(user)).build());
    }

    @Override
    public void deleteUser(DeleteUserArgs request, StreamObserver<DeleteUserResult> responseObserver) {
        super.toGrpcResult(responseObserver, impl.deleteUser(request.getName(), request.getPwd()),
                (user) -> DeleteUserResult.newBuilder().setUser(User_to_GrpcUser(user)).build());
    }

    @Override
    public void searchUsers(SearchUsersArgs request, StreamObserver<GrpcUser> responseObserver) {
        var res = impl.searchUsers(request.getName(), request.getPwd(), request.getQuery());
        if (res.isOK()) {
            for (var u : res.value()) {
                responseObserver.onNext(User_to_GrpcUser(u));
            }
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(errorCodeToStatus(res.error()));
        }
    }
    @Override
    public void verifyUser(VerifyUserArgs request, StreamObserver<GrpcUser> responseObserver) {
        super.toGrpcResult(responseObserver, impl.verifyUser(request.getName()),
                (user) -> User_to_GrpcUser(user));
    }

}
