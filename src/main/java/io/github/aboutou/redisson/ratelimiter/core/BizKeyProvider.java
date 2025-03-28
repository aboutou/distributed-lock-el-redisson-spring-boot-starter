package io.github.aboutou.redisson.ratelimiter.core;


import io.github.aboutou.redisson.common.utils.SpringSpelUtil;
import io.github.aboutou.redisson.ratelimiter.annotation.RateLimit;
import io.github.aboutou.redisson.ratelimiter.annotation.RateLimitKey;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;


/**
 * @author tiny
 */
public class BizKeyProvider implements BeanFactoryAware {

    private static final Logger log = LoggerFactory.getLogger(BizKeyProvider.class);

    private static final TemplateParserContext PARSER_CONTEXT = new TemplateParserContext();

    private final ExpressionParser parser = new SpelExpressionParser();

    private final StandardEvaluationContext evaluationContext = new StandardEvaluationContext();

    private BeanFactory beanFactory;

    public String getKeyName(JoinPoint joinPoint, RateLimit rateLimit) {
        Method method = getMethod(joinPoint);
        List<String> definitionKeys = getSpelDefinitionKey(joinPoint.getTarget(), rateLimit.keys(), method, joinPoint.getArgs());
        List<String> keyList = new ArrayList<>(definitionKeys);
        List<String> parameterKeys = getParameterKey(method.getParameters(), joinPoint.getArgs());
        keyList.addAll(parameterKeys);
        return StringUtils.collectionToDelimitedString(keyList, "", "-", "");
    }

    public Long getRateValue(RateLimit rateLimit) {
        if (StringUtils.hasText(rateLimit.rateExpression())) {
            String value = parser.parseExpression(resolve(rateLimit.rateExpression()), PARSER_CONTEXT)
                    .getValue(String.class);
            if (value != null) {
                return Long.parseLong(value);
            }
        }
        return rateLimit.rate();
    }

    private Method getMethod(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        if (method.getDeclaringClass().isInterface()) {
            try {
                method = joinPoint.getTarget().getClass().getDeclaredMethod(signature.getName(),
                        method.getParameterTypes());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        return method;
    }

    @SuppressWarnings("ConstantConditions")
    private List<String> getSpelDefinitionKey(Object rootObject, String[] definitionKeys, Method method, Object[] parameterValues) {
        List<String> definitionKeyList = new ArrayList<>();
        for (String definitionKey : definitionKeys) {
            if (!ObjectUtils.isEmpty(definitionKey)) {
                Object objKey = SpringSpelUtil.getKeys(rootObject, method, parameterValues, beanFactory, definitionKey);
                definitionKeyList.add(ObjectUtils.nullSafeToString(objKey));
            }
        }
        return definitionKeyList;
    }

    private List<String> getParameterKey(Parameter[] parameters, Object[] parameterValues) {
        List<String> parameterKey = new ArrayList<>();
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].getAnnotation(RateLimitKey.class) != null) {
                RateLimitKey keyAnnotation = parameters[i].getAnnotation(RateLimitKey.class);
                if (keyAnnotation.value().isEmpty()) {
                    Object parameterValue = parameterValues[i];
                    parameterKey.add(ObjectUtils.nullSafeToString(parameterValue));
                } else {
                    StandardEvaluationContext context = new StandardEvaluationContext(parameterValues[i]);
                    Object key = parser.parseExpression(keyAnnotation.value()).getValue(context);
                    parameterKey.add(ObjectUtils.nullSafeToString(key));
                }
            }
        }
        return parameterKey;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        this.evaluationContext.setBeanResolver(new BeanFactoryResolver(beanFactory));
    }

    private String resolve(String value) {
        return ((ConfigurableBeanFactory) this.beanFactory).resolveEmbeddedValue(value);
    }
}
