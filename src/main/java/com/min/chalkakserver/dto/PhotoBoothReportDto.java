package com.min.chalkakserver.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhotoBoothReportDto {

    @NotBlank(message = "네컷사진관 이름은 필수입니다")
    @Size(min = 2, max = 100)
    private String name;

    @Size(max = 50)
    private String brand;

    @Size(max = 50)
    private String series;

    @NotBlank(message = "주소는 필수입니다")
    @Size(max = 255)
    private String address;

    @Size(max = 255)
    private String roadAddress;

    @NotNull(message = "위도는 필수입니다")
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private Double latitude;

    @NotNull(message = "경도는 필수입니다")
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private Double longitude;

    @Size(max = 1000)
    private String description;

    @Size(max = 500)
    private String priceInfo;

    @Size(max = 1000)
    private String reporterName;

    @Email
    private String reporterEmail;
}
