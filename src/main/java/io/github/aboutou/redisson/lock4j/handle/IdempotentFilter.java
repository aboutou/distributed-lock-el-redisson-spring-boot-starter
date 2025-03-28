package io.github.aboutou.redisson.lock4j.handle;

import io.github.aboutou.redisson.lock4j.annotation.IdempotentWhitelist;
import io.github.aboutou.redisson.lock4j.lock.DistributedLockCallback;
import io.github.aboutou.redisson.lock4j.lock.DistributedLockTemplate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.condition.PathPatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPattern;

import javax.annotation.PostConstruct;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 接口幂等
 *
 * @author tiny
 */
public class IdempotentFilter implements Filter, Ordered, ApplicationRunner {

    private static final Log log = LogFactory.getLog(IdempotentFilter.class);

    private AntPathMatcher antPathMatcher = new AntPathMatcher();
    private UrlPathHelper urlPathHelper = new UrlPathHelper();
    private List<RequestUrl> whiteListUrls = new ArrayList<>();
    private static final List<HttpMethod> HTTP_METHOD_LIST = Arrays.asList(HttpMethod.GET, HttpMethod.OPTIONS, HttpMethod.HEAD, HttpMethod.TRACE);

    @Value("${spring.application.name}")
    private String appName;
    @Autowired
    private DistributedLockTemplate lockTemplate;
    @Autowired(required = false)
    private List<HandlerExceptionResolver> handlerExceptionResolvers;
    @Autowired(required = false)
    @Qualifier("idempotentUrls")
    private List<RequestUrl> idempotentUrls;

    @Value("${spring.redis.lock.idempotent.filter.wait.time:4}")
    private Long waitTime = 4L;
    @Value("${spring.redis.lock.idempotent.filter.lease.time:30}")
    private Long leaseTime = 30L;
    @Autowired(required = false)
    private List<RequestMappingHandlerMapping> handlerMappings;

    @PostConstruct
    public void init() {
        whiteListUrls.add(new RequestUrl(null, "/actuator/**"));
        if (idempotentUrls != null) {
            whiteListUrls.addAll(idempotentUrls);
        }
        if (handlerExceptionResolvers != null) {
            AnnotationAwareOrderComparator.sort(handlerExceptionResolvers);
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String path = urlPathHelper.getLookupPathForRequest(request);
        String method = request.getMethod().toUpperCase();
        boolean b = HTTP_METHOD_LIST.stream().anyMatch(p -> p.matches(method));
        if (b) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }
        String authorization = request.getHeader("Authorization");
        if (StringUtils.isEmpty(authorization)) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }
        if (!CollectionUtils.isEmpty(whiteListUrls)) {
            for (RequestUrl whiteListUrl : whiteListUrls) {
                String url = whiteListUrl.url;
                HttpMethod method1 = whiteListUrl.method;
                boolean m = method1 == null ? true : method1.matches(method);
                boolean match = antPathMatcher.match(url, path);
                if (match && m) {
                    log.info("匹配白名单，直接放行{" + path + "}!");
                    filterChain.doFilter(servletRequest, servletResponse);
                    return;
                }
            }
        }
        StringJoiner joiner = new StringJoiner("|");
        joiner.add(appName).add(authorization).add(path).add(method);
        String key = joiner.toString();
        if (log.isDebugEnabled()) {
            log.debug("幂等key[" + key + "]");
        }
        String keyStr = appName + ":" + DigestUtils.md5DigestAsHex(key.getBytes());
        try {
            this.lockTemplate.tryLock(new DistributedLockCallback<Void>() {
                @Override
                public Void process() throws Throwable {
                    filterChain.doFilter(servletRequest, servletResponse);
                    return null;
                }

                @Override
                public List<String> getLockKey() {
                    return Arrays.asList(keyStr);
                }
            }, waitTime, leaseTime, TimeUnit.SECONDS, false);
        } catch (IOException e) {
            if (resolveException(request, response, e) != null) {
                return;
            }
            throw e;
        } catch (ServletException e) {
            if (resolveException(request, response, e) != null) {
                return;
            }
            throw e;
        } catch (Throwable e) {
            if (e instanceof Exception) {
                if (resolveException(request, response, (Exception) e) != null) {
                    return;
                }
            }
            throw new RuntimeException(e);
        }
    }

    private ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Exception e) {
        if (handlerExceptionResolvers != null) {
            for (HandlerExceptionResolver handlerExceptionResolver : handlerExceptionResolvers) {
                ModelAndView modelAndView = handlerExceptionResolver.resolveException(request, response, null, e);
                if (modelAndView != null) {
                    return modelAndView;
                }
            }
        }
        return null;
    }


    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (CollectionUtils.isEmpty(handlerMappings)) {
            return;
        }
        for (RequestMappingHandlerMapping handlerMapping : handlerMappings) {
            Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();
            if (handlerMethods == null) {
                return;
            }
            for (RequestMappingInfo mappingInfo : handlerMethods.keySet()) {
                HandlerMethod handlerMethod = handlerMethods.get(mappingInfo);
                Method method = handlerMethod.getMethod();
                IdempotentWhitelist idempotentWhitelist = AnnotationUtils.findAnnotation(method, IdempotentWhitelist.class);
                if (idempotentWhitelist == null) {
                    continue;
                }
                RequestMethodsRequestCondition methodsCondition = mappingInfo.getMethodsCondition();
                Set<RequestMethod> methods = methodsCondition.getMethods();
                Set<String> patterns = getPatterns(mappingInfo);
                if (patterns == null) {
                    continue;
                }
                for (RequestMethod requestMethod : methods) {
                    for (String pattern : patterns) {
                        RequestUrl url = new RequestUrl(HttpMethod.valueOf(requestMethod.name()), pattern);
                        whiteListUrls.add(url);
                    }
                }
            }
        }
    }

    private Set<String> getPatterns(RequestMappingInfo mappingInfo) {
        Set<String> result = new LinkedHashSet<>();
        PatternsRequestCondition patternsCondition = mappingInfo.getPatternsCondition();
        Set<String> set = Optional.ofNullable(patternsCondition).map(PatternsRequestCondition::getPatterns).orElseGet(LinkedHashSet::new);
        result.addAll(set);
        PathPatternsRequestCondition pathPatternsCondition = mappingInfo.getPathPatternsCondition();
        Set<PathPattern> set1 = Optional.ofNullable(pathPatternsCondition).map(PathPatternsRequestCondition::getPatterns).orElseGet(LinkedHashSet::new);
        if (!CollectionUtils.isEmpty(set1)) {
            result.addAll(set1.stream().map(PathPattern::getPatternString).collect(Collectors.toList()));
        }
        return result;
    }

    public static class RequestUrl {
        private String url;
        private HttpMethod method;

        public RequestUrl(HttpMethod method, String url) {
            this.method = method;
            this.url = url;
        }

    }
}
