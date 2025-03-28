package io.github.aboutou.redisson.ratelimiter.exception;

/**
 * @author tiny
 */
public class ExecuteFunctionException extends RuntimeException {

    public ExecuteFunctionException(String message, Throwable cause) {
        super(message, cause);
    }
}