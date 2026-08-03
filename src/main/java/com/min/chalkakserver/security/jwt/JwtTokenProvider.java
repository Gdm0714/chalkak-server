package com.min.chalkakserver.security.jwt;

import com.min.chalkakserver.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-validity:3600000}")  // 1시간 (밀리초)
    private long accessTokenValidity;

    @Value("${jwt.refresh-token-validity:1209600000}")  // 14일 (밀리초)
    private long refreshTokenValidity;

    private SecretKey key;

    @PostConstruct
    protected void init() {
        // JWT Secret 환경변수 필수 검증
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException(
                "JWT_SECRET 환경변수가 설정되지 않았습니다. " +
                "최소 32자 이상의 시크릿 키를 설정해주세요."
            );
        }
        if (secretKey.length() < 32) {
            throw new IllegalStateException(
                "JWT_SECRET은 최소 32자(256비트) 이상이어야 합니다. " +
                "현재 길이: " + secretKey.length()
            );
        }
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        log.info("JWT Token Provider initialized successfully");
    }

    public String createAccessToken(User user) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + accessTokenValidity);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("nickname", user.getNickname())
                .claim("role", user.getRole().name())
                .claim("provider", user.getProvider().name())
                .issuedAt(now)
                .expiration(validity)
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(User user) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + refreshTokenValidity);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(validity)
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SignatureException | MalformedJwtException e) {
            // io.jsonwebtoken.security.SignatureException 이어야 한다.
            // java.lang.SecurityException 으로 잡으면 서명 불일치가 호출자까지 전파되어
            // /api/auth/refresh 가 401 대신 500을 반환한다.
            log.warn("Invalid JWT signature: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            // 액세스 토큰 만료는 정상 흐름(클라이언트가 갱신)이므로 에러 레벨로 남기지 않는다.
            log.debug("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
        } catch (JwtException e) {
            // 위에서 다루지 않은 모든 JJWT 예외도 검증 실패(false)로 수렴시킨다.
            log.warn("Invalid JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.parseLong(claims.getSubject());
    }

    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getAccessTokenValidity() {
        return accessTokenValidity;
    }

    public long getRefreshTokenValidity() {
        return refreshTokenValidity;
    }

    public Date getRefreshTokenExpiryDate() {
        return new Date(System.currentTimeMillis() + refreshTokenValidity);
    }
}
