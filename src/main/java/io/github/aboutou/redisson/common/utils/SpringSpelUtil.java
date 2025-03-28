package io.github.aboutou.redisson.common.utils;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @author tiny
 */
public class SpringSpelUtil {

    private final static ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    private final static Map<AnnotatedElementKey, Method> targetMethodCache = new ConcurrentHashMap<>(64);


    public static String getkeyForSpel(Object rootObject, Method method, Object[] arguments, String key) {
        ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(rootObject, method, arguments,
                parameterNameDiscoverer);
        ExpressionParser parser = new SpelExpressionParser();
        String value = parser.parseExpression(key).getValue(context, String.class);
        return value;
    }

    public static Object getKeys(Object rootObject, Method method, Object[] arguments, String key) {
        return getKeys(rootObject, method, arguments, null, key, null);
    }

    public static Object getKeys(Object rootObject, Method method, Object[] arguments, String key, Map<String, Object> variables) {
        return getKeys(rootObject, method, arguments, null, key, variables);
    }

    public static Object getKeys(Object rootObject, Method method, Object[] arguments, BeanFactory beanFactory, String key) {
        return getKeys(rootObject, method, arguments, beanFactory, key, null);
    }

    public static Object getKeys(Object obj, Method method, Object[] arguments, BeanFactory beanFactory, String key, Map<String, Object> variables) {
        Object rootObject = obj;
        if (!(obj instanceof MethodExpressionRootObject)) {
            rootObject = new MethodExpressionRootObject(obj, obj.getClass(), method, method.getName(), arguments);
        }
        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(rootObject, method, arguments,
                parameterNameDiscoverer);
        if (beanFactory != null) {
            context.setBeanResolver(new BeanFactoryResolver(beanFactory));
        }
        if (variables != null) {
            context.setVariables(variables);
        }
        ExpressionParser parser = new SpelExpressionParser();
        Object value = parser.parseExpression(key).getValue(context);
        return value;
    }

    public static <T> T getKeys(Object obj, Method method, Object[] arguments, BeanFactory beanFactory, String key, Map<String, Object> variables, Class<T> desiredResultType) {
        Object rootObject = obj;
        if (!(obj instanceof MethodExpressionRootObject)) {
            rootObject = new MethodExpressionRootObject(obj, obj.getClass(), method, method.getName(), arguments);
        }
        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(rootObject, method, arguments, parameterNameDiscoverer);
        if (beanFactory != null) {
            context.setBeanResolver(new BeanFactoryResolver(beanFactory));
        }
        if (variables != null) {
            context.setVariables(variables);
        }
        ExpressionParser parser = new SpelExpressionParser();
        T value = parser.parseExpression(key).getValue(context, desiredResultType);
        return value;
    }

    public static Method getTargetMethod(Class<?> targetClass, Method method) {
        AnnotatedElementKey methodKey = new AnnotatedElementKey(method, targetClass);
        Method targetMethod = targetMethodCache.get(methodKey);
        if (targetMethod == null) {
            targetMethod = AopUtils.getMostSpecificMethod(method, targetClass);
            if (targetMethod == null) {
                targetMethod = method;
            }
            targetMethodCache.put(methodKey, targetMethod);
        }
        return targetMethod;
    }


    static class MethodExpressionRootObject {

        private final Object target;

        private final Class<?> targetClass;

        private final Method method;

        private final String methodName;

        private final Object[] args;

        MethodExpressionRootObject(Object target, Class<?> targetClass, Method method, String methodName, Object[] args) {
            this.target = target;
            this.targetClass = targetClass;
            this.method = method;
            this.methodName = methodName;
            this.args = args;
        }

        public Method getMethod() {
            return method;
        }

        public Object[] getArgs() {
            return args;
        }

        public Object getTarget() {
            return target;
        }

        public Class<?> getTargetClass() {
            return targetClass;
        }

        public String getMethodName() {
            return methodName;
        }
    }
}
