package com.min.chalkakserver.dto.notification;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DeviceTokenRequestDto {

    @NotBlank
    private String fcmToken;

    @NotBlank
    private String platform;
}
