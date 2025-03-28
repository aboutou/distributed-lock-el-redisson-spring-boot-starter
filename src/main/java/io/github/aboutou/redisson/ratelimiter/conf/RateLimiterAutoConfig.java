package io.github.aboutou.redisson.ratelimiter.conf;


import io.github.aboutou.redisson.lock4j.conf.RedissonLockAutoConfig;
import io.github.aboutou.redisson.ratelimiter.core.BizKeyProvider;
import io.github.aboutou.redisson.ratelimiter.core.RateLimiterAspect;
import io.github.aboutou.redisson.ratelimiter.core.RateLimiterAspectService;
import io.github.aboutou.redisson.ratelimiter.service.RateLimiterService;
import io.github.aboutou.redisson.ratelimiter.service.impl.RedissonRateLimiterServiceImpl;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * @author tiny
 */
@Configuration
@AutoConfigureAfter({RedisAutoConfiguration.class, RedissonLockAutoConfig.class})
@ConditionalOnClass({RedissonClient.class})
@ConditionalOnProperty(value = "spring.redisson.ratelimiter.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimiterAutoConfig {

    @Bean
    public RedissonRateLimiterServiceImpl redissonRateLimiterServiceImpl(RedissonClient client) {
        return new RedissonRateLimiterServiceImpl(client);
    }

    @Bean
    public RateLimiterAspect rateLimitAspectHandler(RateLimiterService rateLimiterService, RateLimiterAspectService lockInfoProvider) {
        return new RateLimiterAspect(rateLimiterService, lockInfoProvider);
    }

    @Bean
    public RateLimiterAspectService rateLimiterAspectService() {
        return new RateLimiterAspectService();
    }

    @Bean
    public BizKeyProvider bizKeyProvider() {
        return new BizKeyProvider();
    }

}
