package io.github.aboutou.redisson.lock4j.exception;


/**
 * @author tiny
 */
public class RedissonLockException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public RedissonLockException() {
		super();
	}

	public RedissonLockException(String message) {
		super(message);
	}

	public RedissonLockException(String message, Throwable cause) {
		super(message, cause);
	}

	public RedissonLockException(Throwable cause) {
		super(cause);
	}

	public RedissonLockException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
