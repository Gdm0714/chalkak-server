package com.min.chalkakserver.service;

import com.min.chalkakserver.entity.User;
import com.min.chalkakserver.entity.User.AuthProvider;
import com.min.chalkakserver.exception.AuthException;
import com.min.chalkakserver.repository.RefreshTokenRepository;
import com.min.chalkakserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 이메일 기반 비밀번호 재설정 및 가입 수단(provider) 조회 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final String CODE_KEY_PREFIX = "pwreset:code:";
    private static final String TOKEN_KEY_PREFIX = "pwreset:token:";
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration TOKEN_TTL = Duration.ofMinutes(10);

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 비밀번호 재설정 요청 - 인증코드 발송
     *
     * 이메일 계정이 없으면 발송하지 않고 오류로 알린다. 오지 않는 메일을 기다리게 하는 것보다
     * 잘못 입력했음을 바로 알려주는 편이 낫다. 계정 열거(enumeration)는 이 방식으로 가능해지지만,
     * 공개 API인 `POST /api/auth/find-provider`가 이미 같은 정보를 반환하므로 여기서 숨겨도
     * 실질적인 보호가 되지 않는다.
     */
    public void requestReset(String email) {
        userRepository.findByEmailAndProvider(email, AuthProvider.EMAIL)
            .ifPresentOrElse(user -> {
                String code = generateCode();
                redisTemplate.opsForValue().set(CODE_KEY_PREFIX + email, code, CODE_TTL);
                emailService.sendPasswordResetCode(email, code);
                // 로컬 테스트용: 실제 SMTP 없이도 코드 확인 가능하도록 로그 출력
                log.info("[DEV] 비밀번호 재설정 인증코드 생성: email={}, code={}", email, code);
            }, () -> {
                throw notRegistered(email);
            });
    }

    /**
     * 이메일 계정이 없을 때의 오류. 소셜로만 가입된 이메일이면 그 사실을 알려준다
     * (비밀번호가 없는 계정이라 재설정 자체가 성립하지 않는다).
     */
    private AuthException notRegistered(String email) {
        List<String> socialProviders = findProviders(email);

        if (socialProviders.isEmpty()) {
            log.info("비밀번호 재설정 요청 - 가입되지 않은 이메일: {}", email);
            return new AuthException("가입되지 않은 이메일입니다. 이메일 주소를 확인해주세요.", "NOT_FOUND");
        }

        String labels = socialProviders.stream()
            .map(PasswordResetService::providerLabel)
            .collect(Collectors.joining(", "));
        log.info("비밀번호 재설정 요청 - 소셜 가입 계정: email={}, providers={}", email, socialProviders);
        return new AuthException(
            labels + "(으)로 가입된 계정입니다. 해당 수단으로 로그인해주세요.", "CONFLICT");
    }

    private static String providerLabel(String provider) {
        return switch (provider) {
            case "KAKAO" -> "카카오";
            case "NAVER" -> "네이버";
            case "APPLE" -> "Apple";
            default -> provider;
        };
    }

    /**
     * 인증코드 검증 - 성공 시 리셋 토큰 발급
     */
    public String verifyCode(String email, String code) {
        Object stored = redisTemplate.opsForValue().get(CODE_KEY_PREFIX + email);
        if (stored == null || !Objects.equals(stored.toString(), code)) {
            throw new AuthException("인증코드가 올바르지 않거나 만료되었습니다", "UNAUTHORIZED");
        }

        String resetToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(TOKEN_KEY_PREFIX + resetToken, email, TOKEN_TTL);
        redisTemplate.delete(CODE_KEY_PREFIX + email);

        log.info("비밀번호 재설정 인증코드 검증 성공: {}", email);
        return resetToken;
    }

    /**
     * 리셋 토큰으로 새 비밀번호 확정
     */
    @Transactional
    public void confirmReset(String resetToken, String newPassword) {
        Object emailObj = redisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + resetToken);
        if (emailObj == null) {
            throw new AuthException("리셋 토큰이 올바르지 않거나 만료되었습니다", "UNAUTHORIZED");
        }
        String email = emailObj.toString();

        User user = userRepository.findByEmailAndProvider(email, AuthProvider.EMAIL)
            .orElseThrow(() -> new AuthException("사용자를 찾을 수 없습니다", "NOT_FOUND"));

        user.updatePassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        redisTemplate.delete(TOKEN_KEY_PREFIX + resetToken);

        // 비밀번호 변경 시 모든 기기 로그아웃 (기존 refresh token 무효화)
        refreshTokenRepository.deleteAllByUser(user);

        log.info("비밀번호 재설정 완료: userId={}", user.getId());
    }

    /**
     * 이메일로 가입된 provider 목록 조회
     */
    @Transactional(readOnly = true)
    public List<String> findProviders(String email) {
        return userRepository.findAllByEmail(email).stream()
            .map(user -> user.getProvider().name())
            .distinct()
            .collect(Collectors.toList());
    }

    private String generateCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }
}
