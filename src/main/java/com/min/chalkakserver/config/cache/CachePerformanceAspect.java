package com.min.chalkakserver.config.cache;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Aspect
@Component
public class CachePerformanceAspect {
    
    private static final Logger log = LoggerFactory.getLogger(CachePerformanceAspect.class);
    
    @Around("@annotation(cacheable)")
    public Object measureCachePerformance(ProceedingJoinPoint joinPoint, Cacheable cacheable) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        Object result = joinPoint.proceed();
        
        stopWatch.stop();
        
        String methodName = joinPoint.getSignature().getName();
        String cacheName = cacheable.value().length > 0 ? cacheable.value()[0] : "unknown";
        long executionTime = stopWatch.getTotalTimeMillis();
        
        if (executionTime > 100) {
            log.warn("캐시 [{}] 메서드 {} 실행 시간: {}ms (캐시 미스 가능성)", cacheName, methodName, executionTime);
        } else if (executionTime < 10) {
            log.info("캐시 [{}] 메서드 {} 실행 시간: {}ms (캐시 히트)", cacheName, methodName, executionTime);
        } else {
            log.debug("캐시 [{}] 메서드 {} 실행 시간: {}ms", cacheName, methodName, executionTime);
        }
        
        return result;
    }
}
