package com.min.chalkakserver.service;

import com.min.chalkakserver.entity.User;
import com.min.chalkakserver.entity.User.AuthProvider;
import com.min.chalkakserver.exception.AuthException;
import com.min.chalkakserver.exception.EmailSendException;
import com.min.chalkakserver.repository.RefreshTokenRepository;
import com.min.chalkakserver.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetService 테스트")
class PasswordResetServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private static final String EMAIL = "user@test.com";

    private User emailUser() {
        return User.builder()
            .email(EMAIL)
            .nickname("tester")
            .provider(AuthProvider.EMAIL)
            .providerId(EMAIL)
            .role(User.Role.USER)
            .password("encodedPassword")
            .build();
    }

    private User socialUser(AuthProvider provider) {
        return User.builder()
            .email(EMAIL)
            .nickname("tester")
            .provider(provider)
            .providerId("social-id")
            .role(User.Role.USER)
            .build();
    }

    @Test
    @DisplayName("이메일 계정이 있으면 인증코드를 저장하고 메일을 발송한다")
    void sendsCodeWhenEmailAccountExists() {
        given(userRepository.findByEmailAndProvider(EMAIL, AuthProvider.EMAIL))
            .willReturn(Optional.of(emailUser()));
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        passwordResetService.requestReset(EMAIL);

        verify(valueOperations).set(eq("pwreset:code:" + EMAIL), anyString(), any());
        verify(emailService).sendPasswordResetCode(eq(EMAIL), anyString());
    }

    @Test
    @DisplayName("메일 발송이 실패하면 503으로 알리고 인증코드를 저장하지 않는다")
    void reportsFailureWhenMailSendFails() {
        given(userRepository.findByEmailAndProvider(EMAIL, AuthProvider.EMAIL))
            .willReturn(Optional.of(emailUser()));
        willThrow(new EmailSendException("boom", new RuntimeException()))
            .given(emailService).sendPasswordResetCode(eq(EMAIL), anyString());

        assertThatThrownBy(() -> passwordResetService.requestReset(EMAIL))
            .isInstanceOf(AuthException.class)
            .satisfies(
                e -> assertThat(((AuthException) e).getCode()).isEqualTo("SERVICE_UNAVAILABLE"));

        // 쓸 수 없는 코드가 남지 않아야 한다.
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("가입되지 않은 이메일이면 NOT_FOUND로 알리고 메일을 보내지 않는다")
    void rejectsUnregisteredEmail() {
        given(userRepository.findByEmailAndProvider(EMAIL, AuthProvider.EMAIL))
            .willReturn(Optional.empty());
        given(userRepository.findAllByEmail(EMAIL)).willReturn(List.of());

        assertThatThrownBy(() -> passwordResetService.requestReset(EMAIL))
            .isInstanceOf(AuthException.class)
            .satisfies(e -> assertThat(((AuthException) e).getCode()).isEqualTo("NOT_FOUND"));

        verify(emailService, never()).sendPasswordResetCode(anyString(), anyString());
    }

    @Test
    @DisplayName("소셜로만 가입된 이메일이면 CONFLICT로 가입 수단을 알려준다")
    void rejectsSocialOnlyEmail() {
        given(userRepository.findByEmailAndProvider(EMAIL, AuthProvider.EMAIL))
            .willReturn(Optional.empty());
        given(userRepository.findAllByEmail(EMAIL))
            .willReturn(List.of(socialUser(AuthProvider.KAKAO)));

        assertThatThrownBy(() -> passwordResetService.requestReset(EMAIL))
            .isInstanceOf(AuthException.class)
            .hasMessageContaining("카카오")
            .satisfies(e -> assertThat(((AuthException) e).getCode()).isEqualTo("CONFLICT"));

        verify(emailService, never()).sendPasswordResetCode(anyString(), anyString());
    }
}
