package com.min.chalkakserver.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialUserInfo {
    private String id;
    private String email;
    private String nickname;
    private String profileImageUrl;
}
