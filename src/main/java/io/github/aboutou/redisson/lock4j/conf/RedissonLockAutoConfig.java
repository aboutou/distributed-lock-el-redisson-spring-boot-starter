package io.github.aboutou.redisson.lock4j.conf;

import io.github.aboutou.redisson.lock4j.aop.DistributedLockAspect;
import io.github.aboutou.redisson.lock4j.handle.IdempotentFilter;
import io.github.aboutou.redisson.lock4j.handle.SpringBeanContext;
import io.github.aboutou.redisson.lock4j.lock.DistributedLockTemplate;
import io.github.aboutou.redisson.lock4j.lock.impl.SingleDistributedLockTemplate;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.MasterSlaveServersConfig;
import org.redisson.spring.starter.RedissonAutoConfiguration;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.redisson.spring.starter.RedissonProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import javax.servlet.Filter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Tiny
 */
@Configuration
// @AutoConfigureAfter(RedissonAutoConfiguration.class)
@AutoConfigureBefore(RedissonAutoConfiguration.class)
@ConditionalOnClass({RedissonClient.class, RedissonAutoConfiguration.class})
@EnableConfigurationProperties({RedissonProperties.class, RedisProperties.class})
public class RedissonLockAutoConfig {


    private static final String REDIS_PROTOCOL_PREFIX = "redis://";
    private static final String REDISS_PROTOCOL_PREFIX = "rediss://";

    @Value("${spring.application.name:}")
    private String appName;

    @Value("${spring.redis.nodes:}")
    private String redisHost;
    @Autowired
    private RedissonProperties redissonProperties;

    @Autowired
    private RedisProperties redisProperties;
    @Autowired
    private ApplicationContext ctx;

    @Autowired(required = false)
    private List<RedissonAutoConfigurationCustomizer> redissonAutoConfigurationCustomizers;

    @Primary
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redisson() {
        Config config = null;
        Method clusterMethod = ReflectionUtils.findMethod(RedisProperties.class, "getCluster");
        Method timeoutMethod = ReflectionUtils.findMethod(RedisProperties.class, "getTimeout");
        Object timeoutValue = ReflectionUtils.invokeMethod(timeoutMethod, redisProperties);
        int timeout;
        if (null == timeoutValue) {
            timeout = 10000;
        } else if (!(timeoutValue instanceof Integer)) {
            Method millisMethod = ReflectionUtils.findMethod(timeoutValue.getClass(), "toMillis");
            timeout = ((Long) ReflectionUtils.invokeMethod(millisMethod, timeoutValue)).intValue();
        } else {
            timeout = (Integer) timeoutValue;
        }
        if (!StringUtils.isEmpty(redisHost)) {
            String[] nodes = convert(Arrays.asList(redisHost.split(",")));
            if (nodes != null && nodes.length > 0) {
                config = new Config();
                MasterSlaveServersConfig masterSlaveServersConfig = config.useMasterSlaveServers()
                        .setDatabase(redisProperties.getDatabase())
                        .setConnectTimeout(timeout)
                        .setPassword(redisProperties.getPassword())
                        .setPingConnectionInterval((int) TimeUnit.MINUTES.toMillis(3))
                        .setSubscriptionConnectionPoolSize(16)
                        .setMasterConnectionPoolSize(10)
                        .setMasterConnectionMinimumIdleSize(5)
                        .setSlaveConnectionPoolSize(10)
                        .setSlaveConnectionMinimumIdleSize(2)
                        .setClientName(appName)
                        ;
                for (int i = 0; i < nodes.length; i++) {
                    if (i == 0) {
                        masterSlaveServersConfig.setMasterAddress(nodes[i]);
                    } else {
                        masterSlaveServersConfig.addSlaveAddress(nodes[i]);
                    }
                }
            }
        } else if (redissonProperties.getConfig() != null) {
            try {
                config = Config.fromYAML(redissonProperties.getConfig());
            } catch (IOException e) {
                try {
                    config = Config.fromJSON(redissonProperties.getConfig());
                } catch (IOException e1) {
                    throw new IllegalArgumentException("Can't parse config", e1);
                }
            }
        } else if (redissonProperties.getFile() != null) {
            try {
                InputStream is = getConfigStream();
                config = Config.fromYAML(is);
            } catch (IOException e) {
                // trying next format
                try {
                    InputStream is = getConfigStream();
                    config = Config.fromJSON(is);
                } catch (IOException e1) {
                    throw new IllegalArgumentException("Can't parse config", e1);
                }
            }
        } else if (redisProperties.getSentinel() != null) {
            Method nodesMethod = ReflectionUtils.findMethod(RedisProperties.Sentinel.class, "getNodes");
            Object nodesValue = ReflectionUtils.invokeMethod(nodesMethod, redisProperties.getSentinel());
            String[] nodes;
            if (nodesValue instanceof String) {
                nodes = convert(Arrays.asList(((String) nodesValue).split(",")));
            } else {
                nodes = convert((List<String>) nodesValue);
            }
            config = new Config();
            config.useSentinelServers()
                    .setMasterName(redisProperties.getSentinel().getMaster())
                    .addSentinelAddress(nodes)
                    .setDatabase(redisProperties.getDatabase())
                    .setConnectTimeout(timeout)
                    .setPassword(redisProperties.getPassword())
                    .setMasterConnectionMinimumIdleSize(10)
                    .setSubscriptionConnectionPoolSize(16)
                    .setMasterConnectionPoolSize(10)
                    .setMasterConnectionMinimumIdleSize(5)
                    .setSlaveConnectionPoolSize(10)
                    .setSlaveConnectionMinimumIdleSize(2)
                    .setClientName(appName);
        } else if (clusterMethod != null && ReflectionUtils.invokeMethod(clusterMethod, redisProperties) != null && redisProperties.getCluster() != null && !CollectionUtils.isEmpty(redisProperties.getCluster().getNodes())) {
            Object clusterObject = ReflectionUtils.invokeMethod(clusterMethod, redisProperties);
            Method nodesMethod = ReflectionUtils.findMethod(clusterObject.getClass(), "getNodes");
            List<String> nodesObject = (List) ReflectionUtils.invokeMethod(nodesMethod, clusterObject);
            String[] nodes = convert(nodesObject);
            config = new Config();
            config.useClusterServers()
                    .addNodeAddress(nodes)
                    .setConnectTimeout(timeout)
                    .setPassword(redisProperties.getPassword());
        } else {
            config = new Config();
            String prefix = REDIS_PROTOCOL_PREFIX;
            Method method = ReflectionUtils.findMethod(RedisProperties.class, "isSsl");
            if (method != null && (Boolean) ReflectionUtils.invokeMethod(method, redisProperties)) {
                prefix = REDISS_PROTOCOL_PREFIX;
            }

            config.useSingleServer()
                    .setAddress(prefix + redisProperties.getHost() + ":" + redisProperties.getPort())
                    .setConnectTimeout(timeout)
                    .setDatabase(redisProperties.getDatabase())
                    .setPassword(redisProperties.getPassword())
                    .setPingConnectionInterval((int) TimeUnit.MINUTES.toMillis(3))
                    .setConnectionMinimumIdleSize(2)
                    .setSubscriptionConnectionPoolSize(10)
                    .setConnectionPoolSize(8)
                    .setClientName(appName)
                    //.setSubscriptionConnectionMinimumIdleSize()
            ;
        }

        if (redissonAutoConfigurationCustomizers != null) {
            for (RedissonAutoConfigurationCustomizer customizer : redissonAutoConfigurationCustomizers) {
                customizer.customize(config);
            }
        }
        redissonAutoConfigurationCustomizer().customize(config);
        return Redisson.create(config);
    }

    private InputStream getConfigStream() throws IOException {
        Resource resource = ctx.getResource(redissonProperties.getFile());
        InputStream is = resource.getInputStream();
        return is;
    }

    @Bean
    public DistributedLockTemplate initDistributedLockTemplate(@Autowired RedissonClient redisson) {
        return new SingleDistributedLockTemplate(redisson);
    }

    @Bean
    public SpringBeanContext redissonSpringBeanContext() {
        return new SpringBeanContext();
    }

    //@Bean
   // @Order()
    public RedissonAutoConfigurationCustomizer redissonAutoConfigurationCustomizer() {
        return (config) -> {
            config.setThreads(4);
            config.setNettyThreads(12);
            //config.setLockWatchdogTimeout(10 * 1000);
        };
    }

    private String[] convert(List<String> nodesObject) {
        List<String> nodes = new ArrayList<String>(nodesObject.size());
        for (String node : nodesObject) {
            if (!node.startsWith(REDIS_PROTOCOL_PREFIX) && !node.startsWith(REDISS_PROTOCOL_PREFIX)) {
                nodes.add(REDIS_PROTOCOL_PREFIX + node);
            } else {
                nodes.add(node);
            }
        }
        return nodes.toArray(new String[nodes.size()]);
    }

    /**
     * 分布式锁
     */
    @Configuration
    //@Import(RedissonLockAutoConfig.class)
    public static class DistributedLockConfig {
        @Bean
        public DistributedLockAspect initDistributedLockAspect() {
            return new DistributedLockAspect();
        }
    }


    /**
     * 幂等拦截
     */
    @Configuration
    @ConditionalOnClass(Filter.class)
    //@Import(RedissonLockAutoConfig.class)
    public static class IdempotentRedissonLockConfig {

        @Bean
        @ConditionalOnMissingBean({IdempotentFilter.class})
        @ConditionalOnProperty(value = "spring.redis.redisson.idempotent.enable", havingValue = "true", matchIfMissing = true)
        public IdempotentFilter idempotentFilter() {
            return new IdempotentFilter();
        }

    }
}
