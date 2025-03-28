package io.github.aboutou.redisson.lock4j.annotation;

import java.lang.annotation.*;

/**
 * 幂等白名单
 *
 * @author tiny
 * 
 * @since 2023/3/20 下午4:13
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface IdempotentWhitelist {
}
