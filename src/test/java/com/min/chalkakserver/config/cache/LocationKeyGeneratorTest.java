package com.min.chalkakserver.config.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LocationKeyGenerator 테스트")
class LocationKeyGeneratorTest {

    private final LocationKeyGenerator keyGenerator = new LocationKeyGenerator();

    @Test
    @DisplayName("위도, 경도, 반경으로 캐시 키를 생성한다")
    void generate_Success() {
        Object key = keyGenerator.generate(null, null, 37.50123, 127.03964, 3.0);
        assertThat(key).isEqualTo("lat:37.501_lon:127.040_rad:3.0");
    }

    @Test
    @DisplayName("소수점 3자리로 절삭하여 키를 생성한다")
    void generate_TruncatesToThreeDecimalPlaces() {
        Object key1 = keyGenerator.generate(null, null, 37.50111, 127.03911, 3.0);
        Object key2 = keyGenerator.generate(null, null, 37.50199, 127.03999, 3.0);
        // 37.501과 37.502, 127.039와 127.040 - 근접하지만 절삭 결과가 다를 수 있음
        assertThat(key1.toString()).startsWith("lat:37.501");
        assertThat(key2.toString()).startsWith("lat:37.502");
    }

    @Test
    @DisplayName("음수 위도/경도도 정상 처리한다")
    void generate_NegativeCoordinates() {
        Object key = keyGenerator.generate(null, null, -33.868, -151.209, 5.0);
        assertThat(key.toString()).contains("lat:-33.868");
        assertThat(key.toString()).contains("lon:-151.209");
    }

    @Test
    @DisplayName("파라미터가 3개 미만이면 IllegalArgumentException이 발생한다")
    void generate_InsufficientParams_ThrowsException() {
        assertThatThrownBy(() -> keyGenerator.generate(null, null, 37.5, 127.0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("동일 위치에 대해 동일한 키를 생성한다")
    void generate_SameLocation_SameKey() {
        Object key1 = keyGenerator.generate(null, null, 37.5012, 127.0396, 3.0);
        Object key2 = keyGenerator.generate(null, null, 37.5012, 127.0396, 3.0);
        assertThat(key1).isEqualTo(key2);
    }
}
