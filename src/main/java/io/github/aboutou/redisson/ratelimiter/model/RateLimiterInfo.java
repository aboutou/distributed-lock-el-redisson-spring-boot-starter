package io.github.aboutou.redisson.ratelimiter.model;


import java.util.concurrent.TimeUnit;

/**
 * @author tiny
 */
public class RateLimiterInfo {

    private String key;
    /**
     * 时间窗口流量数量,
     * 限流速率，默认每秒30
     */
    private long rate;
    /**
     * 时间窗口，最小单位秒，如 2s，2h , 2d
     * java.time.Duration 的写法
     * 限流速率
     */
    private long rateInterval;

    private TimeUnit timeUnit;
    /**
     * 超时时间，固定单位是秒
     */
    private long timeout;

    /**
     * 是否重置
     */
    private boolean resetting;

    private RateType rateType = RateType.SINGLE;


    public RateLimiterInfo(String key, long rate, long rateInterval) {
        this(key, rate, rateInterval, TimeUnit.SECONDS);
    }

    public RateLimiterInfo(String key, long rate, long rateInterval, TimeUnit timeUnit) {
        this.key = key;
        this.rate = rate;
        this.rateInterval = rateInterval;
        this.timeUnit = timeUnit;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public long getRate() {
        return rate;
    }

    public void setRate(long rate) {
        this.rate = rate;
    }

    public long getRateInterval() {
        return rateInterval;
    }

    public void setRateInterval(long rateInterval) {
        this.rateInterval = rateInterval;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    public RateType getRateType() {
        return rateType;
    }

    public void setRateType(RateType rateType) {
        this.rateType = rateType;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public boolean isResetting() {
        return resetting;
    }

    public void setResetting(boolean resetting) {
        this.resetting = resetting;
    }
}
