package io.github.aboutou.redisson.ratelimiter.core;


import io.github.aboutou.redisson.ratelimiter.annotation.RateLimit;
import io.github.aboutou.redisson.ratelimiter.exception.ExecuteFunctionException;
import io.github.aboutou.redisson.ratelimiter.model.RateLimiterInfo;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;


/**
 * @author tiny
 */
public class RateLimiterAspectService {

    private static final String NAME_PREFIX = "RateLimiter:";
    private static final Logger logger = LoggerFactory.getLogger(RateLimiterAspectService.class);

    @Autowired
    private BizKeyProvider bizKeyProvider;

    RateLimiterInfo getRateLimiterInfo(JoinPoint joinPoint, RateLimit rateLimit) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String businessKeyName = bizKeyProvider.getKeyName(joinPoint, rateLimit);
        String rateLimitKey = getName(signature) + businessKeyName;
        if (StringUtils.hasLength(rateLimit.customKeyFunction())) {
            try {
                rateLimitKey = getName(signature) + this.executeFunction(rateLimit.customKeyFunction(), joinPoint).toString();
            } catch (Throwable throwable) {
                logger.info("Gets the custom Key exception and degrades it to the default Key:{}", rateLimit, throwable);
            }
        }
        long rate = bizKeyProvider.getRateValue(rateLimit);
        // long rateInterval = DurationStyle.detectAndParse(rateLimit.rateInterval()).getSeconds();
        long rateInterval = rateLimit.rateInterval();
        TimeUnit timeUnit = rateLimit.timeUnit();
        // RateIntervalUnit rateIntervalUnit = RateIntervalUnit.valueOf(timeUnit.name());
        RateLimiterInfo rateLimiterInfo = new RateLimiterInfo(rateLimitKey, rate, rateInterval);
        if (timeUnit != null) {
            rateLimiterInfo.setTimeUnit(timeUnit);
        }
        rateLimiterInfo.setTimeout(rateLimit.timeout());
        rateLimiterInfo.setRateType(rateLimit.rateType());
        return rateLimiterInfo;
    }

    /**
     * 执行自定义函数
     */
    public Object executeFunction(String fallbackName, JoinPoint joinPoint) throws Throwable {
        // prepare invocation context
        Method currentMethod = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Object target = joinPoint.getTarget();
        Method handleMethod = null;
        try {
            handleMethod = joinPoint.getTarget().getClass().getDeclaredMethod(fallbackName, currentMethod.getParameterTypes());
            handleMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Illegal annotation param customLockTimeoutStrategy", e);
        }
        Object[] args = joinPoint.getArgs();

        // invoke
        Object res = null;
        try {
            res = handleMethod.invoke(target, args);
        } catch (IllegalAccessException e) {
            throw new ExecuteFunctionException("Fail to invoke custom lock timeout handler: " + fallbackName, e);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
        return res;
    }

    /**
     * 获取基础的限流 key
     */
    private String getName(MethodSignature signature) {
        return NAME_PREFIX + String.format("%s.%s", signature.getDeclaringTypeName(), signature.getMethod().getName());
    }

}
