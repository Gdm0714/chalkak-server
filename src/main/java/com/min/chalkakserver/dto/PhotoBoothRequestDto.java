package com.min.chalkakserver.dto;

import com.min.chalkakserver.entity.PhotoBooth;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhotoBoothRequestDto {
    
    @NotBlank(message = "네컷사진관 이름은 필수입니다")
    @Size(min = 2, max = 100, message = "이름은 2자 이상 100자 이하여야 합니다")
    private String name;
    
    @Size(max = 50, message = "브랜드명은 50자 이하여야 합니다")
    private String brand;
    
    @Size(max = 50, message = "시리즈명은 50자 이하여야 합니다")
    private String series;
    
    @NotBlank(message = "주소는 필수입니다")
    @Size(max = 255, message = "주소는 255자 이하여야 합니다")
    private String address;
    
    @Size(max = 255, message = "도로명 주소는 255자 이하여야 합니다")
    private String roadAddress;
    
    @NotNull(message = "위도는 필수입니다")
    @DecimalMin(value = "-90.0", message = "위도는 -90도 이상이어야 합니다")
    @DecimalMax(value = "90.0", message = "위도는 90도 이하여야 합니다")
    private Double latitude;
    
    @NotNull(message = "경도는 필수입니다")
    @DecimalMin(value = "-180.0", message = "경도는 -180도 이상이어야 합니다")
    @DecimalMax(value = "180.0", message = "경도는 180도 이하여야 합니다")
    private Double longitude;
    
    @Size(max = 100, message = "영업시간은 100자 이하여야 합니다")
    private String operatingHours;
    
    @Pattern(regexp = "^(\\d{2,3}-\\d{3,4}-\\d{4})?$", message = "전화번호 형식이 올바르지 않습니다 (예: 02-1234-5678)")
    private String phoneNumber;
    
    @Size(max = 1000, message = "설명은 1000자 이하여야 합니다")
    private String description;
    
    @Size(max = 500, message = "가격 정보는 500자 이하여야 합니다")
    private String priceInfo;
    
    // Entity 변환 메서드
    public PhotoBooth toEntity() {
        return PhotoBooth.builder()
                .name(this.name)
                .brand(this.brand)
                .series(this.series)
                .address(this.address)
                .roadAddress(this.roadAddress)
                .latitude(this.latitude)
                .longitude(this.longitude)
                .operatingHours(this.operatingHours)
                .phoneNumber(this.phoneNumber)
                .description(this.description)
                .priceInfo(this.priceInfo)
                .build();
    }
}
