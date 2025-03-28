package io.github.aboutou.redisson.lock4j;

import io.github.aboutou.redisson.lock4j.conf.RedissonLockAutoConfig.IdempotentRedissonLockConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author tiny
 */
@Documented
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({IdempotentRedissonLockConfig.class})
public @interface EnableIdempotentLock {
}
