package com.min.chalkakserver.util;

import com.min.chalkakserver.service.PhotoBoothService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

/**
 * 위치 검색 성능 벤치마크
 * 프로파일: benchmark로 실행 시 활성화
 * 실행: java -jar app.jar --spring.profiles.active=benchmark
 */
@Component
@Profile("benchmark")
public class LocationSearchBenchmark implements CommandLineRunner {
    
    private static final Logger log = LoggerFactory.getLogger(LocationSearchBenchmark.class);
    
    @Autowired
    private PhotoBoothService photoBoothService;
    
    @Override
    public void run(String... args) throws Exception {
        log.info("=== 위치 검색 성능 벤치마크 시작 ===");
        
        // 테스트 위치들 (서울 주요 지역)
        double[][] testLocations = {
            {37.5565, 126.9239}, // 홍대
            {37.4979, 127.0276}, // 강남
            {37.5636, 126.9869}, // 명동
            {37.5585, 126.9386}, // 신촌
            {37.5410, 127.0695}  // 건대
        };
        
        double[] radiuses = {1.0, 2.0, 3.0, 5.0}; // km
        
        StopWatch totalWatch = new StopWatch();
        totalWatch.start();
        
        for (double[] location : testLocations) {
            for (double radius : radiuses) {
                StopWatch watch = new StopWatch();
                watch.start();
                
                // 첫 번째 호출 (캐시 미스)
                var results1 = photoBoothService.getNearbyPhotoBooths(
                    location[0], location[1], radius);
                watch.stop();
                long firstCallTime = watch.getTotalTimeMillis();
                
                // 두 번째 호출 (캐시 히트)
                watch.start();
                var results2 = photoBoothService.getNearbyPhotoBooths(
                    location[0], location[1], radius);
                watch.stop();
                long secondCallTime = watch.getLastTaskTimeMillis();
                
                log.info("위치: ({}, {}), 반경: {}km", 
                    location[0], location[1], radius);
                log.info("  - 첫 번째 호출 (캐시 미스): {}ms, 결과: {}개", 
                    firstCallTime, results1.size());
                log.info("  - 두 번째 호출 (캐시 히트): {}ms", secondCallTime);
                log.info("  - 성능 개선: {}%", 
                    ((firstCallTime - secondCallTime) * 100.0 / firstCallTime));
            }
        }
        
        totalWatch.stop();
        log.info("=== 벤치마크 완료: 총 소요시간 {}ms ===", 
            totalWatch.getTotalTimeMillis());
    }
}
