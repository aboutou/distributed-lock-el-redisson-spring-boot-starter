package io.github.aboutou.redisson.common.utils;

import io.github.aboutou.redisson.lock4j.handle.SpringBeanContext;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

/**
 * @author tiny
 * 
 * @since 2022/11/7 下午2:29
 */
public class SpringReflectionUtil {


    /**
     * @param serviceName 服务名称
     * @param methodName  方法名称
     * @param params      参数
     * @return
     */
    public static Object springInvokeMethod(String serviceName, String methodName, Object[] params) {
        Object service = SpringBeanContext.getBean(serviceName);
        return springInvokeMethod(service, methodName, params);
    }

    /**
     * @param service    服务类
     * @param methodName 方法名称
     * @param params     参数
     * @return
     */
    public static Object springInvokeMethod(Object service, String methodName, Object[] params) {
        Class<? extends Object>[] paramClass = null;
        if (params != null) {
            int paramsLength = params.length;
            paramClass = new Class[paramsLength];
            for (int i = 0; i < paramsLength; i++) {
                paramClass[i] = params[i].getClass();
            }
        }
        // 找到方法
        Method method = ReflectionUtils.findMethod(service.getClass(), methodName, paramClass);
        // 执行方法
        return ReflectionUtils.invokeMethod(method, service, params);
    }

}
