package io.github.aboutou.redisson.ratelimiter.annotation;

import io.github.aboutou.redisson.ratelimiter.model.RateType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;


/**
 * @author tiny
 */
@Target(value = {ElementType.METHOD})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 时间窗口流量数量,
     * 限流速率，默认每秒30
     *
     * @return rate
     */
    long rate() default 30;

    /**
     * 时间窗口流量数量表达式
     *
     * @return rateExpression
     */
    String rateExpression() default "";

    /**
     * 时间窗口，最小单位秒，如 2s，2h , 2d
     * java.time.Duration 的写法
     * 限流速率
     * @return rateInterval
     */
    //String rateInterval();
    long rateInterval() default 1;

    /**
     * 获取key
     *
     * @return keys
     */
    String[] keys() default {};

    /**
     * 超时时间，默认5
     * @return
     */
    long timeout() default 5L;

    /**
     * 超时时间单位，默认秒
     * @return
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 限流后的自定义回退后的拒绝逻辑
     *
     * @return fallback
     */
    String fallbackFunction() default "";

    /**
     * 自定义业务 key 的 Function
     *
     * @return key
     */
    String customKeyFunction() default "";


    /**
     *  TOTAL 所有客户端加总限流
     *  SINGLE 每个客户端单独计算流量
     * @return
     */
    RateType rateType() default RateType.SINGLE;

}
