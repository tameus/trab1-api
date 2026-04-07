package sd2526.trab.server.grpc;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import sd2526.trab.api.User;
import sd2526.trab.api.grpc.GrpcUsersGrpc;
import sd2526.trab.api.grpc.Users;
import sd2526.trab.server.java.JavaUsers;
import sd2526.trab.api.java.Result;

import java.util.List;
import java.util.logging.Logger;

public class GrpcUsersResource extends GrpcUsersGrpc.GrpcUsersImplBase {
    private static final Logger Log = Logger.getLogger(GrpcUsersResource.class.getName());
    private final JavaUsers impl;
    private final String domain;

    public GrpcUsersResource(String domain) {
        this.domain = domain;
        this.impl = new JavaUsers(domain);
    }
    @Override
    public void postUser(Users.GrpcUser request, StreamObserver<Users.PostUserResult> responseObserver) {
        /*User user = new User(request.getName(), request.getPwd(), request.getDisplayName(), request.getDomain());

        Result<String> res = impl.postUser(user);

        if (res.isOK()) {
            responseObserver.onNext(Users.PostUserResult.newBuilder().setUserAddress(res.value()).build());
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(mapError(res.error()).asException());
        }*/
        try {
            // 1. Criar o objeto User Java
            User user = new User(request.getName(), request.getPwd(), request.getDisplayName(), request.getDomain());

            // 2. Chamar a implementação JavaUsers
            Result<String> res = impl.postUser(user);

            if (res.isOK()) {
                System.out.println("DEBUG: [JavaUsers retornou OK] -> " + res.value());

                // 3. Construir a resposta gRPC
                Users.PostUserResult reply = Users.PostUserResult.newBuilder()
                        .setUserAddress(res.value()) // Garante que o proto tem 'userAddress'
                        .build();

                // 4. Enviar e fechar a stream
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
                System.out.println("DEBUG: [Resposta enviada com sucesso ao cliente]");

            } else {
                System.err.println("DEBUG: [JavaUsers retornou ERRO] -> " + res.error());
                // Mapeia o erro e envia via gRPC
                responseObserver.onError(mapError(res.error()).asException());
            }
        } catch (Throwable t) {
            // ISTO VAI APANHAR QUALQUER CRASH (NullPointer, etc.)
            System.err.println("DEBUG: [CRASH NO RESOURCE]");
            t.printStackTrace();

            // Envia o erro real para o cliente não ficar à espera
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Erro interno no servidor: " + t.getMessage())
                    .asException());
        }

    }

    @Override
    public void getUser(Users.GetUserArgs request, StreamObserver<Users.GetUserResult> responseObserver) {
        Result<User> res = impl.getUser(request.getName(), request.getPwd());

        if (res.isOK()) {
            User u = res.value();
            Users.GrpcUser gUser = Users.GrpcUser.newBuilder()
                    .setName(u.getName())
                    .setDisplayName(u.getDisplayName())
                    .setDomain(u.getDomain())
                    .build();

            responseObserver.onNext(Users.GetUserResult.newBuilder().setUser(gUser).build());
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(mapError(res.error()).asException());
        }
    }

    @Override
    public void updateUser(Users.UpdateUserArgs request, StreamObserver<Users.UpdateUserResult> responseObserver) {
        Users.GrpcUser info = request.getInfo();
        User user = new User(info.getName(), info.getPwd(), info.getDisplayName(), info.getDomain());

        Result<User> res = impl.updateUser(request.getName(), request.getPwd(), user);

        if (res.isOK()) {
            User u = res.value();
            Users.GrpcUser gUser = Users.GrpcUser.newBuilder()
                    .setName(u.getName())
                    .setPwd(u.getPwd())
                    .setDisplayName(u.getDisplayName())
                    .setDomain(u.getDomain())
                    .build();
            responseObserver.onNext(Users.UpdateUserResult.newBuilder().setUser(gUser).build());
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(mapError(res.error()).asException());
        }
    }

    @Override
    public void deleteUser(Users.DeleteUserArgs request, StreamObserver<Users.DeleteUserResult> responseObserver) {
        Result<User> res = impl.deleteUser(request.getName(), request.getPwd());

        if (res.isOK()) {
            User u = res.value();
            Users.GrpcUser gUser = Users.GrpcUser.newBuilder()
                    .setName(u.getName())
                    .setPwd(u.getPwd())
                    .setDisplayName(u.getDisplayName())
                    .setDomain(u.getDomain())
                    .build();
            responseObserver.onNext(Users.DeleteUserResult.newBuilder().setUser(gUser).build());
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(mapError(res.error()).asException());
        }
    }

    @Override
    public void searchUsers(Users.SearchUsersArgs request, StreamObserver<Users.GrpcUser> responseObserver) {
        Result<List<User>> res = impl.searchUsers(request.getName(), request.getPwd(), request.getQuery());

        if (res.isOK()) {
            for (User u : res.value()) {
                responseObserver.onNext(Users.GrpcUser.newBuilder()
                        .setName(u.getName())
                        .setDisplayName(u.getDisplayName())
                        .setDomain(u.getDomain())
                        .build());
            }
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(mapError(res.error()).asException());
        }
    }


    private Status mapError(Result.ErrorCode error) {
        return switch (error) {
            case NOT_FOUND -> Status.NOT_FOUND;
            case FORBIDDEN -> Status.PERMISSION_DENIED;
            case CONFLICT -> Status.ALREADY_EXISTS;
            case BAD_REQUEST -> Status.INVALID_ARGUMENT;
            default -> Status.INTERNAL;
        };
    }
}
