package io.github.aboutou.redisson.lock4j.lock;

import java.util.List;

/**
 * 分布式锁回调接口
 *
 * @author tiny
 */
public interface DistributedLockCallback<T> {

    /**
     * 调用者必须在此方法中实现需要加分布式锁的业务逻辑
     *
     * @return T
     * @throws Throwable
     */
    T process() throws Throwable;

    /**
     * 得到分布式锁名称
     *
     * @return
     */
    List<String> getLockKey();
}
