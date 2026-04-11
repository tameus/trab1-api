package sd2526.trab.clients.grpc;

import java.net.URI;
import java.util.function.Supplier;
import java.util.logging.Logger;

import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Result.ErrorCode;
import sd2526.trab.clients.AbstractClient;

public class GrpcClient extends AbstractClient {

    final private URI serverURI;
    final protected Channel channel;

    protected GrpcClient(URI serverURI, Logger logger) {
        super(logger);
        this.serverURI = serverURI;

        this.channel = ManagedChannelBuilder.forAddress(this.serverURI.getHost(), this.serverURI.getPort())
                .usePlaintext().build();
    }

    @Override
    protected boolean isRetryable(RuntimeException e) {
        return e instanceof StatusRuntimeException sre &&
               sre.getStatus().getCode() == Status.Code.UNAVAILABLE;
    }

    @Override
    protected ErrorCode toErrorCode(RuntimeException e) {
        if (e instanceof StatusRuntimeException sre)
            return statusToErrorCode(sre.getStatus());
        return ErrorCode.INTERNAL_ERROR;
    }

    //retornam valor
    protected <T> Result<T> processResponse(Supplier<T> func) {
        return reTry(() -> Result.ok(func.get()));
    }

    //void
    protected Result<Void> processResponse(Runnable proc) {
        return reTry(() -> { proc.run(); return Result.ok(); });
    }

    protected static ErrorCode statusToErrorCode(Status status) {
        return switch (status.getCode()) {
            case OK -> ErrorCode.OK;
            case NOT_FOUND -> ErrorCode.NOT_FOUND;
            case ALREADY_EXISTS -> ErrorCode.CONFLICT;
            case PERMISSION_DENIED -> ErrorCode.FORBIDDEN;
            case INVALID_ARGUMENT -> ErrorCode.BAD_REQUEST;
            case UNIMPLEMENTED -> ErrorCode.NOT_IMPLEMENTED;
            case DEADLINE_EXCEEDED -> ErrorCode.TIMEOUT;
            default -> ErrorCode.INTERNAL_ERROR;
        };
    }
}
