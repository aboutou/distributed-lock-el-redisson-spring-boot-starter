package io.github.aboutou.redisson.ratelimiter.exception;


/**
 * @author tiny
 */
public class RateLimitException extends RuntimeException {

    public RateLimitException(String message) {
        super(message);
    }

}
