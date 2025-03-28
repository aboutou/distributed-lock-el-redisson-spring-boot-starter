package io.github.aboutou.redisson.lock4j.aop;

import io.github.aboutou.redisson.lock4j.annotation.DistributedLock;
import io.github.aboutou.redisson.lock4j.lock.DistributedLockCallback;
import io.github.aboutou.redisson.lock4j.lock.DistributedLockTemplate;
import io.github.aboutou.redisson.common.utils.SpringSpelUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.Ordered;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 *
 * see springaop
 * @author tiny
 */
@Aspect
public class DistributedLockAspect implements Ordered {

    private static final Logger log = LoggerFactory.getLogger(DistributedLockAspect.class);

    @Autowired
    private DistributedLockTemplate lockTemplate;
    // @Autowired
    // private HandlerExceptionResolver handlerExceptionResolver;
    @Value("${spring.application.name}")
    private String appName;

    @Pointcut("@annotation(io.github.aboutou.redisson.lock4j.annotation.DistributedLocks) || @annotation(io.github.aboutou.redisson.lock4j.annotation.DistributedLock)")
    public void distributedLockAspect() {
    }

    @Around(value = "distributedLockAspect()")
    public Object doAround(ProceedingJoinPoint pjp) throws Throwable {
        Object target = pjp.getTarget();
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        Object[] arguments = pjp.getArgs();
        DistributedLock[] annotations = method.getAnnotationsByType(DistributedLock.class);
        List<String> lockKey = getLockKey(target, method, arguments, annotations);
        if (log.isDebugEnabled()) {
            log.debug("分布式锁的lockKey size=[{}], lockKey=[{}]", lockKey.size(), lockKey);
        }
        if (CollectionUtils.isEmpty(lockKey)) {
            return pjp.proceed();
        }
        return lock(pjp, lockKey, annotations);
    }

    public List<String> getLockKey(Object target, Method method, Object[] args, DistributedLock[] annotations) {
        Objects.requireNonNull(method);
        List<String> lockKeys = new ArrayList<>();
        for (DistributedLock annotation : annotations) {
            String param = annotation.key();
            if (!"".equals(param) && null != param) {
                Object value = SpringSpelUtil.getKeys(target, method, args, param);
                lockKeys.addAll(getSpelValue(annotation, value));
                continue;
            }
            // 兜底了
            String sb = getSpelValue(annotation, target.getClass().getName() + "#" + method.getName());
            lockKeys.add(sb);
        }
        return lockKeys.stream().distinct().collect(Collectors.toList());
    }

    private List<String> getSpelValue(DistributedLock annotation, Object value) {
        List<String> lockKeys = new ArrayList<>();
        if (null == value) {
            return lockKeys;
        }
        if (value instanceof List) {
            lockKeys.addAll(getSpelValue1(annotation, (List) value));
        } else {
            String sb = getSpelValue(annotation, String.valueOf(value));
            lockKeys.add(sb);
        }
        return lockKeys;
    }

    private List<String> getSpelValue1(DistributedLock annotation, List value) {
        List<String> lockKeys = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        List lists = value;
        for (Object list : lists) {
            if (list instanceof List) {
                lockKeys.addAll(getSpelValue1(annotation, (List) list));
                continue;
            }
            String sb = getSpelValue(annotation, String.valueOf(list));
            keys.add(sb);
        }
        lockKeys.addAll(keys);
        return lockKeys;
    }

    private String getSpelValue(DistributedLock annotation, String list) {
        StringBuilder sb = new StringBuilder();
        sb.append(appName);
        sb.append(annotation.separator());
        sb.append(annotation.lockKeyPre());
        sb.append(annotation.separator());
        sb.append(annotation.lockName());
        sb.append(annotation.separator());
        sb.append(list);
        sb.append(annotation.separator());
        sb.append(annotation.lockKeySuffix());
        return sb.toString();
    }


    /**
     * <pre>
     *         Stream<DistributedLock> lockStream = Stream.of(annotations);
     *         DistributedLock annotation = lockStream.findFirst().get();
     *         boolean fairLock = lockStream.map(p -> p.fairLock()).allMatch(p -> Boolean.TRUE.booleanValue() == p);
     *         boolean tryLock = lockStream.map(p -> p.tryLock()).allMatch(p -> Boolean.TRUE.booleanValue() == p);
     *         long waitTime = lockStream.mapToLong(DistributedLock::waitTime).summaryStatistics().getAverage();
     * </pre>
     *
     * @param pjp
     * @param lockKey
     * @param annotations
     * @return
     * @throws Throwable
     */
    private Object lock(ProceedingJoinPoint pjp, List<String> lockKey, DistributedLock[] annotations) throws Throwable {
        DistributedLock annotation = annotations[0];
        boolean fairLock = annotation.fairLock();
        boolean tryLock = annotation.tryLock();
        long waitTime = annotation.waitTime();
        long leaseTime = annotation.leaseTime() <= 0 ? -1 : annotation.leaseTime();
        TimeUnit timeUnit = annotation.timeUnit();
        DistributedLockCallback<Object> callback = new DistributedLockCallback<Object>() {
            @Override
            public Object process() throws Throwable {
                return proceed(pjp);
            }

            @Override
            public List<String> getLockKey() {
                return lockKey;
            }
        };
        if (tryLock) {
            return tryLock(callback, waitTime, leaseTime, timeUnit, fairLock);
        } else {
            return lock(callback, leaseTime, timeUnit, fairLock);
        }
    }

    public Object lock(DistributedLockCallback<Object> callback, Long release, TimeUnit timeUnit, boolean fairLock) throws Throwable {
        return lockTemplate.lock(callback, release, timeUnit, fairLock);
    }

    public Object tryLock(DistributedLockCallback<Object> callback, long waitTime, long leaseTime, TimeUnit timeUnit, boolean fairLock) throws Throwable {
        return lockTemplate.tryLock(callback, waitTime, leaseTime, timeUnit, fairLock);
    }

    private Object proceed(ProceedingJoinPoint pjp) throws Throwable {
        try {
            return pjp.proceed();
        } catch (RuntimeException e1) {
            throw NestedExceptionUtils.getMostSpecificCause(e1);
        } catch (Throwable throwable) {
            /*ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null && throwable instanceof Exception) {
                Exception e = (Exception) throwable;
                HttpServletRequest request = requestAttributes.getRequest();
                HttpServletResponse response = requestAttributes.getResponse();
                if (handlerExceptionResolver.resolveException(request, response, null, e) == null) {
                    throw new RuntimeException(e);
                }
            }*/
            throw throwable;
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

}
