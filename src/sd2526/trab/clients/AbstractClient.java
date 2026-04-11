package sd2526.trab.clients;

import java.util.function.Supplier;
import java.util.logging.Logger;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Result.ErrorCode;

public abstract class AbstractClient {

    protected static final int MAX_RETRIES = 3;
    protected static final long RETRY_SLEEP = 2000;

    protected final Logger logger;

    protected AbstractClient(Logger logger) {
        this.logger = logger;
    }

    protected abstract boolean isRetryable(RuntimeException e);
    protected abstract ErrorCode toErrorCode(RuntimeException e);

    protected <T> Result<T> reTry(Supplier<Result<T>> func) {
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                return func.get();
            } catch (RuntimeException e) {
                if (isRetryable(e)) {
                    logger.info("Retry [%d]: %s".formatted(i, e.getMessage()));
                    sleep(RETRY_SLEEP);
                } else {
                    return Result.error(toErrorCode(e));
                }
            } catch (Exception e) {
                e.printStackTrace();
                return Result.error(ErrorCode.INTERNAL_ERROR);
            }
        }
        return Result.error(ErrorCode.TIMEOUT);
    }

    protected void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
