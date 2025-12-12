package com.min.chalkakserver.service;

import com.min.chalkakserver.dto.PagedResponseDto;
import com.min.chalkakserver.dto.PhotoBoothResponseDto;
import com.min.chalkakserver.entity.PhotoBooth;
import com.min.chalkakserver.exception.InvalidLocationException;
import com.min.chalkakserver.exception.PhotoBoothNotFoundException;
import com.min.chalkakserver.repository.PhotoBoothRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("PhotoBoothService 테스트")
class PhotoBoothServiceTest {

    @Mock
    private PhotoBoothRepository photoBoothRepository;

    @InjectMocks
    private PhotoBoothService photoBoothService;

    private PhotoBooth testPhotoBooth;
    private PhotoBooth testPhotoBooth2;

    @BeforeEach
    void setUp() {
        testPhotoBooth = PhotoBooth.builder()
                .id(1L)
                .name("테스트 사진관")
                .brand("인생네컷")
                .address("서울특별시 강남구 역삼동")
                .roadAddress("서울특별시 강남구 테헤란로 123")
                .latitude(37.5012)
                .longitude(127.0396)
                .build();

        testPhotoBooth2 = PhotoBooth.builder()
                .id(2L)
                .name("테스트 사진관2")
                .brand("하루필름")
                .address("서울특별시 서초구 서초동")
                .roadAddress("서울특별시 서초구 강남대로 456")
                .latitude(37.4920)
                .longitude(127.0300)
                .build();
    }

    @Nested
    @DisplayName("전체 조회 테스트")
    class GetAllPhotoBoothsTest {

        @Test
        @DisplayName("모든 네컷사진관을 조회한다")
        void getAllPhotoBooths_Success() {
            // given
            given(photoBoothRepository.findAll())
                    .willReturn(Arrays.asList(testPhotoBooth, testPhotoBooth2));

            // when
            List<PhotoBoothResponseDto> result = photoBoothService.getAllPhotoBooths();

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("테스트 사진관");
            assertThat(result.get(1).getName()).isEqualTo("테스트 사진관2");
        }

        @Test
        @DisplayName("네컷사진관이 없으면 빈 리스트를 반환한다")
        void getAllPhotoBooths_Empty() {
            // given
            given(photoBoothRepository.findAll())
                    .willReturn(Collections.emptyList());

            // when
            List<PhotoBoothResponseDto> result = photoBoothService.getAllPhotoBooths();

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("페이지네이션으로 네컷사진관을 조회한다")
        void getAllPhotoBoothsPaged_Success() {
            // given
            Page<PhotoBooth> page = new PageImpl<>(
                    Arrays.asList(testPhotoBooth, testPhotoBooth2),
                    Pageable.ofSize(10),
                    2
            );
            given(photoBoothRepository.findAll(any(Pageable.class)))
                    .willReturn(page);

            // when
            PagedResponseDto<PhotoBoothResponseDto> result = 
                    photoBoothService.getAllPhotoBoothsPaged(0, 10);

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isTrue();
        }
    }

    @Nested
    @DisplayName("ID로 조회 테스트")
    class GetPhotoBoothByIdTest {

        @Test
        @DisplayName("ID로 네컷사진관을 조회한다")
        void getPhotoBoothById_Success() {
            // given
            given(photoBoothRepository.findById(1L))
                    .willReturn(Optional.of(testPhotoBooth));

            // when
            PhotoBoothResponseDto result = photoBoothService.getPhotoBoothById(1L);

            // then
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("테스트 사진관");
            assertThat(result.getBrand()).isEqualTo("인생네컷");
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회하면 예외가 발생한다")
        void getPhotoBoothById_NotFound() {
            // given
            given(photoBoothRepository.findById(999L))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> photoBoothService.getPhotoBoothById(999L))
                    .isInstanceOf(PhotoBoothNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("근처 검색 테스트")
    class GetNearbyPhotoBoothsTest {

        @Test
        @DisplayName("근처 네컷사진관을 검색한다")
        void getNearbyPhotoBooths_Success() {
            // given
            double latitude = 37.5012;
            double longitude = 127.0396;
            double radius = 3.0;

            given(photoBoothRepository.findNearbyPhotoBooths(
                    eq(latitude), eq(longitude), eq(radius),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .willReturn(Arrays.asList(testPhotoBooth));

            // when
            List<PhotoBoothResponseDto> result = 
                    photoBoothService.getNearbyPhotoBooths(latitude, longitude, radius);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("테스트 사진관");
        }

        @Test
        @DisplayName("유효하지 않은 위도로 검색하면 예외가 발생한다")
        void getNearbyPhotoBooths_InvalidLatitude() {
            // given
            double invalidLatitude = 100.0; // 위도는 -90 ~ 90
            double longitude = 127.0396;
            double radius = 3.0;

            // when & then
            assertThatThrownBy(() -> 
                    photoBoothService.getNearbyPhotoBooths(invalidLatitude, longitude, radius))
                    .isInstanceOf(InvalidLocationException.class)
                    .hasMessageContaining("위도");
        }

        @Test
        @DisplayName("유효하지 않은 경도로 검색하면 예외가 발생한다")
        void getNearbyPhotoBooths_InvalidLongitude() {
            // given
            double latitude = 37.5012;
            double invalidLongitude = 200.0; // 경도는 -180 ~ 180
            double radius = 3.0;

            // when & then
            assertThatThrownBy(() -> 
                    photoBoothService.getNearbyPhotoBooths(latitude, invalidLongitude, radius))
                    .isInstanceOf(InvalidLocationException.class)
                    .hasMessageContaining("경도");
        }

        @Test
        @DisplayName("유효하지 않은 반경으로 검색하면 예외가 발생한다")
        void getNearbyPhotoBooths_InvalidRadius() {
            // given
            double latitude = 37.5012;
            double longitude = 127.0396;
            double invalidRadius = -1.0; // 반경은 0 초과

            // when & then
            assertThatThrownBy(() -> 
                    photoBoothService.getNearbyPhotoBooths(latitude, longitude, invalidRadius))
                    .isInstanceOf(InvalidLocationException.class)
                    .hasMessageContaining("반경");
        }

        @Test
        @DisplayName("반경이 50km를 초과하면 예외가 발생한다")
        void getNearbyPhotoBooths_RadiusTooLarge() {
            // given
            double latitude = 37.5012;
            double longitude = 127.0396;
            double tooLargeRadius = 100.0; // 최대 50km

            // when & then
            assertThatThrownBy(() -> 
                    photoBoothService.getNearbyPhotoBooths(latitude, longitude, tooLargeRadius))
                    .isInstanceOf(InvalidLocationException.class)
                    .hasMessageContaining("반경");
        }
    }

    @Nested
    @DisplayName("검색 테스트")
    class SearchPhotoBoothsTest {

        @Test
        @DisplayName("키워드로 네컷사진관을 검색한다")
        void searchPhotoBooths_Success() {
            // given
            String keyword = "강남";
            given(photoBoothRepository.findByNameContainingIgnoreCaseOrAddressContainingIgnoreCase(keyword, keyword))
                    .willReturn(Arrays.asList(testPhotoBooth));

            // when
            List<PhotoBoothResponseDto> result = photoBoothService.searchPhotoBooths(keyword);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAddress()).contains("강남");
        }

        @Test
        @DisplayName("브랜드로 네컷사진관을 검색한다")
        void getPhotoBoothsByBrand_Success() {
            // given
            String brand = "인생네컷";
            given(photoBoothRepository.findByBrandContainingIgnoreCase(brand))
                    .willReturn(Arrays.asList(testPhotoBooth));

            // when
            List<PhotoBoothResponseDto> result = photoBoothService.getPhotoBoothsByBrand(brand);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getBrand()).isEqualTo("인생네컷");
        }
    }
}
