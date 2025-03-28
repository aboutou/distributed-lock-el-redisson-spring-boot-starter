package io.github.aboutou.redisson.ratelimiter.core;


import io.github.aboutou.redisson.ratelimiter.annotation.RateLimit;
import io.github.aboutou.redisson.ratelimiter.exception.RateLimitException;
import io.github.aboutou.redisson.ratelimiter.model.RateLimiterInfo;
import io.github.aboutou.redisson.ratelimiter.service.RateLimiterService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;


/**
 * @author tiny
 */
@Aspect
@Order(0)
public class RateLimiterAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterAspect.class);

    private final RateLimiterAspectService rateLimiterAspectService;
    private final RateLimiterService rateLimiterService;

    public RateLimiterAspect(RateLimiterService rateLimiterService, RateLimiterAspectService rateLimiterAspectService) {
        this.rateLimiterAspectService = rateLimiterAspectService;
        this.rateLimiterService = rateLimiterService;
    }

    @Around(value = "@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        RateLimiterInfo limiterInfo = rateLimiterAspectService.getRateLimiterInfo(joinPoint, rateLimit);
        boolean allowed = rateLimiterService.tryLimit(limiterInfo, limiterInfo.getTimeout(), limiterInfo.getTimeUnit());
        if (!allowed) {
            log.info("Trigger current limiting, key:{}", limiterInfo.getKey());
            if (StringUtils.hasLength(rateLimit.fallbackFunction())) {
                return rateLimiterAspectService.executeFunction(rateLimit.fallbackFunction(), joinPoint);
            }
            throw new RateLimitException("请求限频，稍候再试！");
        }
        return joinPoint.proceed();
    }


}
