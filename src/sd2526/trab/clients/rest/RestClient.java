package sd2526.trab.clients.rest;

import java.net.URI;
import java.util.logging.Logger;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Result.ErrorCode;
import sd2526.trab.clients.AbstractClient;

public class RestClient extends AbstractClient {

    private static final int READ_TIMEOUT = 10000;
    private static final int CONNECT_TIMEOUT = 1000;

    final URI serverURI;
    final Client client;
    final ClientConfig config;

    protected WebTarget target;

    public RestClient(URI serverURI, Logger logger) {
        super(logger);
        this.serverURI = serverURI;

        this.config = new ClientConfig();
        this.config.property(ClientProperties.READ_TIMEOUT, READ_TIMEOUT);
        this.config.property(ClientProperties.CONNECT_TIMEOUT, CONNECT_TIMEOUT);

        this.client = ClientBuilder.newClient(config);
        this.target = client.target(serverURI);
    }

    @Override
    protected boolean isRetryable(RuntimeException e) {
        return e instanceof ProcessingException;
    }

    @Override
    protected ErrorCode toErrorCode(RuntimeException e) {
        return ErrorCode.INTERNAL_ERROR;
    }

    //para objetos
    protected <T> Result<T> processResponse(Response r, Class<T> entityType) {
        try {
            var status = r.getStatusInfo().toEnum();
            if (status == Status.OK && r.hasEntity())
                return Result.ok(r.readEntity(entityType));
            return Result.error(getErrorCodeFrom(status.getStatusCode()));
        } finally {
            r.close();
        }
    }
    //para listas de objetos
    protected <T> Result<T> processResponse(Response r, GenericType<T> entityType) {
        try{
            var status = r.getStatusInfo().toEnum();
            if(status == Status.OK && r.hasEntity())
                return Result.ok(r.readEntity(entityType));
            return Result.error(getErrorCodeFrom(status.getStatusCode()));
        }finally{
            r.close();
        }
    }
    //sem corpo, ex delete
    protected Result<Void> processResponse(Response r) {
        try{
            var status = r.getStatusInfo().toEnum();
            if(status == Status.OK || status == Status.NO_CONTENT)
                return Result.ok();
            return Result.error(getErrorCodeFrom(status.getStatusCode()));
        }finally{
            r.close();
        }
    }

    protected static ErrorCode getErrorCodeFrom(int status) {
        return switch (status) {
            case 200, 209 -> ErrorCode.OK;
            case 409 -> ErrorCode.CONFLICT;
            case 403 -> ErrorCode.FORBIDDEN;
            case 404 -> ErrorCode.NOT_FOUND;
            case 400 -> ErrorCode.BAD_REQUEST;
            case 500 -> ErrorCode.INTERNAL_ERROR;
            case 501 -> ErrorCode.NOT_IMPLEMENTED;
            case 503 -> ErrorCode.TIMEOUT;
            default -> ErrorCode.INTERNAL_ERROR;
        };
    }
}
