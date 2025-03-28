package io.github.aboutou.redisson.lock4j.annotation;

import java.lang.annotation.*;

/**
 * <p>
 * 拦截注解	        使用注解个数	getAnnotation	getAnnotationsByType
 * Aop             单个              √	                √
 * Aop             多个              ×	                ×
 * Aops            单个              ×	                ×
 * Aops            多个              ×	                √
 * Aop + Aops	    单个	             √	                √
 * Aop + Aops	    多个	             ×	                √
 * </p>
 * @author tiny
 * 
 * @since 2022/1/19 下午1:56
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface DistributedLocks {

    /**
     * 值
     *
     * @return
     */
    DistributedLock[] value();

}
