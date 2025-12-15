package com.min.chalkakserver.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.min.chalkakserver.dto.auth.SocialUserInfo;
import com.min.chalkakserver.exception.AuthException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocialAuthService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${oauth2.apple.client-id:}")
    private String appleClientId;

    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
    private static final String NAVER_USER_INFO_URL = "https://openapi.naver.com/v1/nid/me";
    private static final String APPLE_PUBLIC_KEYS_URL = "https://appleid.apple.com/auth/keys";
    private static final String APPLE_ISSUER = "https://appleid.apple.com";

    public SocialUserInfo getSocialUserInfo(String provider, String accessToken) {
        return switch (provider.toLowerCase()) {
            case "kakao" -> getKakaoUserInfo(accessToken);
            case "naver" -> getNaverUserInfo(accessToken);
            case "apple" -> getAppleUserInfo(accessToken);
            default -> throw new AuthException("Unsupported provider: " + provider);
        };
    }

    /**
     * 카카오 사용자 정보 조회
     */
    private SocialUserInfo getKakaoUserInfo(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                KAKAO_USER_INFO_URL, HttpMethod.GET, entity, String.class);

            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            
            String id = jsonNode.get("id").asText();
            JsonNode kakaoAccount = jsonNode.get("kakao_account");
            JsonNode profile = kakaoAccount.get("profile");

            String email = kakaoAccount.has("email") ? kakaoAccount.get("email").asText() : null;
            String nickname = profile.has("nickname") ? profile.get("nickname").asText() : null;
            String profileImageUrl = profile.has("profile_image_url") 
                ? profile.get("profile_image_url").asText() : null;

            log.info("Kakao user info retrieved: id={}", id);

            return SocialUserInfo.builder()
                .id(id)
                .email(email)
                .nickname(nickname)
                .profileImageUrl(profileImageUrl)
                .build();

        } catch (Exception e) {
            log.error("Failed to get Kakao user info: {}", e.getMessage());
            throw new AuthException("Failed to get Kakao user info");
        }
    }

    /**
     * 네이버 사용자 정보 조회
     */
    private SocialUserInfo getNaverUserInfo(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                NAVER_USER_INFO_URL, HttpMethod.GET, entity, String.class);

            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            JsonNode responseNode = jsonNode.get("response");

            String id = responseNode.get("id").asText();
            String email = responseNode.has("email") ? responseNode.get("email").asText() : null;
            String nickname = responseNode.has("nickname") ? responseNode.get("nickname").asText() : null;
            String profileImageUrl = responseNode.has("profile_image") 
                ? responseNode.get("profile_image").asText() : null;

            log.info("Naver user info retrieved: id={}", id);

            return SocialUserInfo.builder()
                .id(id)
                .email(email)
                .nickname(nickname)
                .profileImageUrl(profileImageUrl)
                .build();

        } catch (Exception e) {
            log.error("Failed to get Naver user info: {}", e.getMessage());
            throw new AuthException("Failed to get Naver user info");
        }
    }

    /**
     * Apple 사용자 정보 조회 (Identity Token 검증)
     * Apple은 ID Token (JWT)을 직접 검증해야 함
     */
    private SocialUserInfo getAppleUserInfo(String identityToken) {
        try {
            // JWT 헤더에서 kid 추출
            String[] tokenParts = identityToken.split("\\.");
            if (tokenParts.length != 3) {
                throw new AuthException("Invalid Apple identity token format");
            }
            
            String headerJson = new String(Base64.getUrlDecoder().decode(tokenParts[0]));
            JsonNode header = objectMapper.readTree(headerJson);
            String kid = header.get("kid").asText();

            // Apple 공개키 가져오기
            PublicKey publicKey = getApplePublicKey(kid);

            // JWT 검증 및 파싱 (RSA 공개키 사용)
            Claims claims = Jwts.parser()
                .verifyWith((java.security.interfaces.RSAPublicKey) publicKey)
                .requireIssuer(APPLE_ISSUER)  // Issuer 검증 추가
                .requireAudience(appleClientId)  // Audience 검증 (앱의 Bundle ID)
                .build()
                .parseSignedClaims(identityToken)
                .getPayload();

            // 토큰 만료 시간 추가 검증
            if (claims.getExpiration().before(new java.util.Date())) {
                throw new AuthException("Apple identity token has expired");
            }

            String id = claims.getSubject();  // Apple의 고유 사용자 ID
            String email = claims.get("email", String.class);

            log.info("Apple user info retrieved: id={}", id);

            return SocialUserInfo.builder()
                .id(id)
                .email(email)
                .nickname(null)  // Apple은 첫 로그인 시에만 이름 제공
                .profileImageUrl(null)
                .build();

        } catch (AuthException e) {
            throw e;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.error("Apple identity token expired: {}", e.getMessage());
            throw new AuthException("Apple identity token has expired");
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.error("Invalid Apple token signature: {}", e.getMessage());
            throw new AuthException("Invalid Apple token signature");
        } catch (Exception e) {
            log.error("Failed to get Apple user info: {}", e.getMessage());
            throw new AuthException("Failed to get Apple user info");
        }
    }

    /**
     * Apple 공개키 조회
     */
    private PublicKey getApplePublicKey(String kid) {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                APPLE_PUBLIC_KEYS_URL, String.class);
            
            JsonNode keys = objectMapper.readTree(response.getBody()).get("keys");
            
            for (JsonNode key : keys) {
                if (key.get("kid").asText().equals(kid)) {
                    String n = key.get("n").asText();
                    String e = key.get("e").asText();
                    
                    byte[] nBytes = Base64.getUrlDecoder().decode(n);
                    byte[] eBytes = Base64.getUrlDecoder().decode(e);
                    
                    BigInteger modulus = new BigInteger(1, nBytes);
                    BigInteger exponent = new BigInteger(1, eBytes);
                    
                    RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
                    KeyFactory factory = KeyFactory.getInstance("RSA");
                    return factory.generatePublic(spec);
                }
            }
            
            throw new AuthException("Apple public key not found for kid: " + kid);
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get Apple public key: {}", e.getMessage());
            throw new AuthException("Failed to get Apple public key");
        }
    }
}
