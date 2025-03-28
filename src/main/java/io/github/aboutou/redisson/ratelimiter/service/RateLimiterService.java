package io.github.aboutou.redisson.ratelimiter.service;

import io.github.aboutou.redisson.ratelimiter.model.RateLimiterInfo;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * @author tiny
 * 
 * @since 2022/5/24 下午12:41
 */
public interface RateLimiterService {


    /**
     * aaa
     *
     * @param limiterInfo
     * @param timeout
     * @param timeUnit
     * @return
     */
    boolean tryLimit(RateLimiterInfo limiterInfo, long timeout, TimeUnit timeUnit);

    /**
     * aaa
     *
     * @param limiterInfo
     * @param timeout
     * @param timeUnit
     * @param expireAt
     * @return
     */
    boolean tryLimit(RateLimiterInfo limiterInfo, long timeout, TimeUnit timeUnit, Instant expireAt);
}
