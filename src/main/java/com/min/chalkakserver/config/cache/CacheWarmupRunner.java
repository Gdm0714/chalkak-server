package com.min.chalkakserver.config.cache;

import com.min.chalkakserver.service.PhotoBoothService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class CacheWarmupRunner implements ApplicationRunner {
    
    private static final Logger log = LoggerFactory.getLogger(CacheWarmupRunner.class);
    
    @Autowired
    private PhotoBoothService photoBoothService;
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("캐시 워밍업 시작...");
        
        try {
            // 1. 전체 네컷사진관 목록 캐싱
            photoBoothService.getAllPhotoBooths();
            log.info("전체 네컷사진관 목록 캐싱 완료");
            
            // 2. 주요 지역별 근처 네컷사진관 캐싱
            List<Location> majorLocations = Arrays.asList(
                new Location("홍대", 37.5565, 126.9239),
                new Location("강남", 37.4979, 127.0276),
                new Location("명동", 37.5636, 126.9869),
                new Location("신촌", 37.5585, 126.9386),
                new Location("건대", 37.5410, 127.0695),
                new Location("종로", 37.5704, 126.9914),
                new Location("성수", 37.5446, 127.0565),
                new Location("잠실", 37.5131, 127.1002)
            );
            
            for (Location location : majorLocations) {
                photoBoothService.getNearbyPhotoBooths(location.latitude, location.longitude, 2.0);
                log.info("{} 지역 근처 네컷사진관 캐싱 완료", location.name);
            }
            
            // 3. 주요 브랜드별 캐싱
            List<String> majorBrands = Arrays.asList("인생네컷", "포토이즘", "하루필름", "포토그레이", "모노맨션", "포토시그니처", "모드빈티크", "RGB 포토스튜디오", "Plan B Studio", "Photomatic");
            for (String brand : majorBrands) {
                photoBoothService.getPhotoBoothsByBrand(brand);
                log.info("{} 브랜드 네컷사진관 캐싱 완료", brand);
            }
            
            log.info("캐시 워밍업 완료!");
            
        } catch (Exception e) {
            log.error("캐시 워밍업 중 오류 발생: ", e);
        }
    }
    
    private static class Location {
        private final String name;
        private final double latitude;
        private final double longitude;
        
        public Location(String name, double latitude, double longitude) {
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}
