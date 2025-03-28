package io.github.aboutou.redisson.lock4j.lock.impl;

import io.github.aboutou.redisson.lock4j.exception.RedissonLockException;
import io.github.aboutou.redisson.lock4j.lock.DistributedLockCallback;
import io.github.aboutou.redisson.lock4j.lock.DistributedLockTemplate;
import org.redisson.RedissonMultiLock;
import org.redisson.RedissonRedLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author tiny
 */
public class SingleDistributedLockTemplate implements DistributedLockTemplate {

    private RedissonClient redisson;

    public SingleDistributedLockTemplate(RedissonClient redisson) {
        this.redisson = redisson;
    }

    @Override
    public <T> T lock(DistributedLockCallback<T> callback, boolean fairLock) throws Throwable {
        return lock(callback, DEFAULT_TIMEOUT, DEFAULT_TIME_UNIT, fairLock);
    }

    /**
     * 不指定定超时时间，才能使看门狗生效
     *
     * @param callback
     * @param leaseTime 锁超时时间。超时后自动释放锁。
     * @param timeUnit
     * @param fairLock  是否使用公平锁
     * @param <T>
     * @return
     */
    @Override
    public <T> T lock(DistributedLockCallback<T> callback, long leaseTime, TimeUnit timeUnit, boolean fairLock) throws Throwable {
        RLock lock = getLock(callback.getLockKey(), fairLock);
        try {
            if (leaseTime <= 0L) {
                lock.lock();
            } else {
                lock.lock(leaseTime, timeUnit);
            }
            return callback.process();
        } finally {
            unlock(lock);
        }
    }

    @Override
    public <T> T tryLock(DistributedLockCallback<T> callback, boolean fairLock) throws Throwable {
        return tryLock(callback, DEFAULT_WAIT_TIME, DEFAULT_TIMEOUT, DEFAULT_TIME_UNIT, fairLock);
    }

    @Override
    public <T> T tryLock(DistributedLockCallback<T> callback, long waitTime, long leaseTime, TimeUnit timeUnit,
                         boolean fairLock) throws Throwable {
        RLock lock = getLock(callback.getLockKey(), fairLock);
        try {
            if (lock.tryLock(waitTime, leaseTime, timeUnit)) {
                return callback.process();
            }
            throw new RedissonLockException("当前任务正在处理中，请勿重复或频繁操作");
        } finally {
            unlock(lock);
        }
    }

    private RLock getLock(List<String> lockNames, boolean fairLock) {
        List<String> lists = lockNames;
        if (lists.isEmpty()) {
            throw new ArrayStoreException("无效的数组数据");
        } else if (lists.size() == 1) {
            return getLock0(String.valueOf(lists.get(0)), fairLock);
        }
        List<RLock> locks = new ArrayList<>();
        for (Object list : lists) {
            RLock lock0 = getLock0(String.valueOf(list), fairLock);
            locks.add(lock0);
        }
        RLock lock = new RedissonRedLock(locks.toArray(new RLock[0]));
        return lock;
    }

    private RLock getLock0(String lockName, boolean fairLock) {
        RLock lock;
        if (fairLock) {
            lock = redisson.getFairLock(lockName);
        } else {
            lock = redisson.getLock(lockName);
        }
        return lock;
    }

    private void unlock(RLock lock) {
        if (lock instanceof RedissonMultiLock) {
            if (lock != null) {
                lock.unlock();
            }
        } else {
            if (lock != null && lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

}
