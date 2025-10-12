package com.min.chalkakserver.config.cache;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component("locationKeyGenerator")
public class LocationKeyGenerator implements KeyGenerator {
    
    @Override
    public Object generate(Object target, Method method, Object... params) {
        if (params.length >= 3) {
            double latitude = (double) params[0];
            double longitude = (double) params[1];
            double radius = (double) params[2];
            
            // 위도/경도를 소수점 3자리까지만 사용하여 캐시 효율성 향상
            // (약 111m 정확도)
            return String.format("lat:%.3f_lon:%.3f_rad:%.1f", latitude, longitude, radius);
        }
        throw new IllegalArgumentException("Invalid parameters for location key generation");
    }
}
