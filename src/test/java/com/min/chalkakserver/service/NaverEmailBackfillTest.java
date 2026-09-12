package com.min.chalkakserver.service;

import com.min.chalkakserver.dto.auth.AuthResponseDto;
import com.min.chalkakserver.dto.auth.SocialLoginRequestDto;
import com.min.chalkakserver.dto.auth.SocialUserInfo;
import com.min.chalkakserver.entity.RefreshToken;
import com.min.chalkakserver.entity.User;
import com.min.chalkakserver.entity.User.AuthProvider;
import com.min.chalkakserver.repository.RefreshTokenRepository;
import com.min.chalkakserver.repository.UserRepository;
import com.min.chalkakserver.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NaverEmailBackfillTest {
    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private SocialAuthService socialAuthService;
    @InjectMocks private AuthService authService;

    @ParameterizedTest
    @CsvSource(value = {
        "NAVER, NULL, contact@example.com, contact@example.com",
        "NAVER, '', contact@example.com, contact@example.com",
        "NAVER, '   ', contact@example.com, contact@example.com",
        "NAVER, saved@example.com, changed@example.com, saved@example.com",
        "NAVER, NULL, NULL, NULL",
        "NAVER, NULL, '', NULL",
        "NAVER, NULL, '   ', NULL",
        "NAVER, saved@example.com, NULL, saved@example.com",
        "KAKAO, NULL, contact@example.com, NULL",
        "APPLE, NULL, contact@example.com, NULL"
    }, nullValues = "NULL")
    @DisplayName("네이버 기존 계정의 빈 이메일만 제공된 이메일로 보완한다")
    void socialLogin_backfillsOnlyMissingNaverEmail(
            AuthProvider provider, String storedEmail, String suppliedEmail, String expectedEmail) {
        // given
        String providerName = provider.name().toLowerCase(Locale.ROOT);
        User existing = User.builder()
                .provider(provider).providerId("provider-user")
                .email(storedEmail).nickname("before").build();
        SocialUserInfo socialInfo = SocialUserInfo.builder()
                .id("provider-user").email(suppliedEmail)
                .nickname("after").profileImageUrl("https://example.com/profile.png").build();
        given(socialAuthService.getSocialUserInfo(providerName, "test-token")).willReturn(socialInfo);
        given(userRepository.findByProviderAndProviderId(provider, "provider-user"))
                .willReturn(Optional.of(existing));
        given(jwtTokenProvider.createAccessToken(existing)).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(existing)).willReturn("refresh-token");
        given(jwtTokenProvider.getAccessTokenValidity()).willReturn(3600000L);
        given(jwtTokenProvider.getRefreshTokenExpiryDate()).willReturn(new Date(4102444800000L));

        // when
        AuthResponseDto result = authService.socialLogin(SocialLoginRequestDto.builder()
                .provider(providerName).accessToken("test-token").deviceInfo("iOS").build());

        // then
        assertThat(existing.getEmail()).isEqualTo(expectedEmail);
        assertThat(result.getUser().getEmail()).isEqualTo(expectedEmail);
        assertThat(result.getUser().getNickname()).isEqualTo("after");
        assertThat(result.getUser().getProfileImageUrl()).isEqualTo("https://example.com/profile.png");
        verify(userRepository).findByProviderAndProviderId(provider, "provider-user");
        verify(userRepository, never()).save(any(User.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }
}
