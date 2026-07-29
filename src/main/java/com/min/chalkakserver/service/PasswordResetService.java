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
     * 이메일 열거(enumeration) 방지를 위해 계정 존재 여부와 무관하게 정상 반환한다.
     */
    public void requestReset(String email) {
        userRepository.findByEmailAndProvider(email, AuthProvider.EMAIL)
            .ifPresentOrElse(user -> {
                String code = generateCode();
                redisTemplate.opsForValue().set(CODE_KEY_PREFIX + email, code, CODE_TTL);
                emailService.sendPasswordResetCode(email, code);
                // 로컬 테스트용: 실제 SMTP 없이도 코드 확인 가능하도록 로그 출력
                log.info("[DEV] 비밀번호 재설정 인증코드 생성: email={}, code={}", email, code);
            }, () -> log.info("비밀번호 재설정 요청 - 존재하지 않는 이메일 계정 (무시): {}", email));
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
