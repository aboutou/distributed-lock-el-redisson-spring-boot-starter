package io.github.aboutou.redisson.ratelimiter.service.impl;

import io.github.aboutou.redisson.ratelimiter.model.RateLimiterInfo;
import io.github.aboutou.redisson.ratelimiter.model.RateType;
import io.github.aboutou.redisson.ratelimiter.service.RateLimiterService;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateLimiterConfig;
import org.redisson.api.RedissonClient;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author tiny
 * 
 * @since 2022/5/24 下午12:35
 */
public class RedissonRateLimiterServiceImpl implements RateLimiterService {


    private final RedissonClient client;

    public RedissonRateLimiterServiceImpl(RedissonClient client) {
        this.client = client;
    }

    /**
     * 获取限流拦截器
     *
     * @param limiterInfo 1
     * @return RRateLimiter 1
     */
    public RRateLimiter getRateLimiter(RateLimiterInfo limiterInfo) {
        RRateLimiter rateLimiter = client.getRateLimiter(StringUtils.isEmpty(limiterInfo.getKey()) ? "default:limiter" : limiterInfo.getKey());
        // 设置限流
        org.redisson.api.RateType rateType = org.redisson.api.RateType.OVERALL;
        if (RateType.SINGLE.equals(limiterInfo.getRateType())) {
            rateType = (org.redisson.api.RateType.PER_CLIENT);
        }
        TimeUnit timeUnit = limiterInfo.getTimeUnit();
        RateIntervalUnit rateIntervalUnit = RateIntervalUnit.valueOf(timeUnit.name());
        if (rateLimiter.isExists()) {
            RateLimiterConfig rateLimiterConfig = rateLimiter.getConfig();
            // 判断配置是否更新，如果更新，重新加载限流器配置
            if (!Objects.equals(limiterInfo.getRate(), rateLimiterConfig.getRate())
                    || !Objects.equals(timeUnit.toMillis(limiterInfo.getRateInterval()), rateLimiterConfig.getRateInterval())
                    || !Objects.equals(rateType, rateLimiterConfig.getRateType())) {
                if (limiterInfo.isResetting()) {
                    rateLimiter.delete();
                    rateLimiter.trySetRate(rateType, limiterInfo.getRate(), limiterInfo.getRateInterval(), rateIntervalUnit);
                } else {
                    rateLimiter.setRate(rateType, limiterInfo.getRate(), limiterInfo.getRateInterval(), rateIntervalUnit);
                }
            }
        } else {
            rateLimiter.trySetRate(rateType, limiterInfo.getRate(), limiterInfo.getRateInterval(), rateIntervalUnit);
        }
        return rateLimiter;
    }

    @Override
    public boolean tryLimit(RateLimiterInfo limiterInfo, long timeout, TimeUnit timeUnit) {
        RRateLimiter rateLimiter = getRateLimiter(limiterInfo);
        return rateLimiter.tryAcquire(1, timeout, timeUnit);
    }

    @Override
    public boolean tryLimit(RateLimiterInfo limiterInfo, long timeout, TimeUnit timeUnit, Instant expireAt) {
        RRateLimiter rateLimiter = getRateLimiter(limiterInfo);
        boolean b = rateLimiter.tryAcquire(1, timeout, timeUnit);
        if (b) {
            rateLimiter.expire(expireAt);
        }
        return b;
    }
}
