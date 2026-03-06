package com.min.chalkakserver.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.min.chalkakserver.dto.auth.SocialUserInfo;
import com.min.chalkakserver.exception.AuthException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SocialAuthServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SocialAuthService socialAuthService;

    private final ObjectMapper realMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        Field field = SocialAuthService.class.getDeclaredField("appleClientId");
        field.setAccessible(true);
        field.set(socialAuthService, "com.test.app");
    }

    @Test
    @DisplayName("카카오 사용자 정보 조회 성공")
    void getSocialUserInfo_kakao_success() throws Exception {
        // given
        String token = "kakao-access-token";
        String kakaoJson = """
                {"id": "12345", "kakao_account": {"email": "test@kakao.com", "profile": {"nickname": "카카오유저", "profile_image_url": "http://img.kakao.com/profile.jpg"}}}
                """;
        JsonNode kakaoNode = realMapper.readTree(kakaoJson);

        given(restTemplate.exchange(
                anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .willReturn(new ResponseEntity<>(kakaoJson, HttpStatus.OK));
        given(objectMapper.readTree(kakaoJson)).willReturn(kakaoNode);

        // when
        SocialUserInfo result = socialAuthService.getSocialUserInfo("kakao", token);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("12345");
        assertThat(result.getEmail()).isEqualTo("test@kakao.com");
        assertThat(result.getNickname()).isEqualTo("카카오유저");
        assertThat(result.getProfileImageUrl()).isEqualTo("http://img.kakao.com/profile.jpg");
    }

    @Test
    @DisplayName("네이버 사용자 정보 조회 성공")
    void getSocialUserInfo_naver_success() throws Exception {
        // given
        String token = "naver-access-token";
        String naverJson = """
                {"response": {"id": "naver_67890", "email": "test@naver.com", "nickname": "네이버유저", "profile_image": "http://img.naver.com/profile.jpg"}}
                """;
        JsonNode naverNode = realMapper.readTree(naverJson);

        given(restTemplate.exchange(
                anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .willReturn(new ResponseEntity<>(naverJson, HttpStatus.OK));
        given(objectMapper.readTree(naverJson)).willReturn(naverNode);

        // when
        SocialUserInfo result = socialAuthService.getSocialUserInfo("naver", token);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("naver_67890");
        assertThat(result.getEmail()).isEqualTo("test@naver.com");
        assertThat(result.getNickname()).isEqualTo("네이버유저");
        assertThat(result.getProfileImageUrl()).isEqualTo("http://img.naver.com/profile.jpg");
    }

    @Test
    @DisplayName("지원하지 않는 provider → AuthException")
    void getSocialUserInfo_unsupportedProvider_throwsAuthException() {
        // when & then
        assertThatThrownBy(() -> socialAuthService.getSocialUserInfo("unknown", "some-token"))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Unsupported provider");
    }

    @Test
    @DisplayName("카카오 API 호출 실패 → AuthException")
    void getSocialUserInfo_kakaoApiFails_throwsAuthException() {
        // given
        given(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .willThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> socialAuthService.getSocialUserInfo("kakao", "bad-token"))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Failed to get Kakao user info");
    }

    @Test
    @DisplayName("네이버 API 호출 실패 → AuthException")
    void getSocialUserInfo_naverApiFails_throwsAuthException() {
        // given
        given(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .willThrow(new RestClientException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> socialAuthService.getSocialUserInfo("naver", "bad-token"))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Failed to get Naver user info");
    }

    @Test
    @DisplayName("카카오 JSON 파싱 실패 → AuthException")
    void getSocialUserInfo_kakaoJsonParseFails_throwsAuthException() throws Exception {
        // given
        String invalidJson = "not-json";
        given(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .willReturn(new ResponseEntity<>(invalidJson, HttpStatus.OK));
        given(objectMapper.readTree(invalidJson)).willThrow(new RuntimeException("parse error"));

        // when & then
        assertThatThrownBy(() -> socialAuthService.getSocialUserInfo("kakao", "token"))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Failed to get Kakao user info");
    }

    @Test
    @DisplayName("네이버 JSON 파싱 실패 → AuthException")
    void getSocialUserInfo_naverJsonParseFails_throwsAuthException() throws Exception {
        // given
        String invalidJson = "not-json";
        given(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .willReturn(new ResponseEntity<>(invalidJson, HttpStatus.OK));
        given(objectMapper.readTree(invalidJson)).willThrow(new RuntimeException("parse error"));

        // when & then
        assertThatThrownBy(() -> socialAuthService.getSocialUserInfo("naver", "token"))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Failed to get Naver user info");
    }

    @Test
    @DisplayName("대소문자 무관 - KAKAO 대문자도 처리")
    void getSocialUserInfo_kakaoUpperCase_success() throws Exception {
        // given
        String token = "kakao-access-token";
        String kakaoJson = """
                {"id": "99999", "kakao_account": {"email": "upper@kakao.com", "profile": {"nickname": "upper", "profile_image_url": "http://img.kakao.com/upper.jpg"}}}
                """;
        JsonNode kakaoNode = realMapper.readTree(kakaoJson);

        given(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .willReturn(new ResponseEntity<>(kakaoJson, HttpStatus.OK));
        given(objectMapper.readTree(kakaoJson)).willReturn(kakaoNode);

        // when
        SocialUserInfo result = socialAuthService.getSocialUserInfo("KAKAO", token);

        // then
        assertThat(result.getId()).isEqualTo("99999");
    }

    // ===================== Apple 로그인 테스트 =====================

    /**
     * 유효한 형태의 가짜 JWT 토큰을 만드는 헬퍼.
     * 실제 서명 검증 전에 터지는 경로(포맷, JWKS 조회)를 테스트하는 데 사용.
     */
    private String buildFakeJwt(String kidValue) {
        String headerJson = "{\"alg\":\"RS256\",\"kid\":\"" + kidValue + "\"}";
        String payloadJson = "{\"sub\":\"apple_user_123\",\"email\":\"apple@test.com\"}";
        String encodedHeader = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(headerJson.getBytes());
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes());
        return encodedHeader + "." + encodedPayload + ".fakesignature";
    }

    @Test
    @DisplayName("Apple - 토큰 형식 오류 (점 구분자 3개 미만) → AuthException")
    void getSocialUserInfo_apple_invalidTokenFormat_throwsAuthException() {
        // given - 점이 2개뿐인 토큰이 아닌, 점이 1개뿐인 완전히 잘못된 토큰
        String malformedToken = "header.payload"; // 3 parts 필요, 2개뿐

        // when & then
        assertThatThrownBy(() -> socialAuthService.getSocialUserInfo("apple", malformedToken))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Invalid Apple identity token format");
    }

    @Test
    @DisplayName("Apple - 점 없는 단일 문자열 토큰 → AuthException")
    void getSocialUserInfo_apple_noDotsToken_throwsAuthException() {
        // given
        String malformedToken = "notavalidjwtatall";

        // when & then
        assertThatThrownBy(() -> socialAuthService.getSocialUserInfo("apple", malformedToken))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Invalid Apple identity token format");
    }

    @Test
    @DisplayName("Apple - JWKS 서버 호출 실패 (RestClientException) → AuthException")
    void getSocialUserInfo_apple_jwksFetchFails_throwsAuthException() throws Exception {
        // given
        String fakeJwt = buildFakeJwt("test-kid-001");
        String headerJson = "{\"alg\":\"RS256\",\"kid\":\"test-kid-001\"}";
        JsonNode headerNode = realMapper.readTree(headerJson);

        given(objectMapper.readTree(any(String.class))).willReturn(headerNode);
        given(restTemplate.getForEntity(anyString(), eq(String.class)))
                .willThrow(new RestClientException("Apple server unreachable"));

        // when & then
        assertThatThrownBy(() -> socialAuthService.getSocialUserInfo("apple", fakeJwt))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Failed to get Apple public key");
    }

    @Test
    @DisplayName("Apple - JWKS 응답 파싱 실패 → AuthException")
    void getSocialUserInfo_apple_jwksJsonParseFails_throwsAuthException() throws Exception {
        // given
        String fakeJwt = buildFakeJwt("test-kid-002");
        String headerJson = "{\"alg\":\"RS256\",\"kid\":\"test-kid-002\"}";
        JsonNode headerNode = realMapper.readTree(headerJson);
        String badJwksResponse = "not-valid-json";

        given(objectMapper.readTree(headerJson)).willReturn(headerNode);
        given(restTemplate.getForEntity(anyString(), eq(String.class)))
                .willReturn(new ResponseEntity<>(badJwksResponse, HttpStatus.OK));
        given(objectMapper.readTree(badJwksResponse))
                .willThrow(new RuntimeException("JSON parse error"));

        // when & then
        assertThatThrownBy(() -> socialAuthService.getSocialUserInfo("apple", fakeJwt))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Failed to get Apple public key");
    }

    @Test
    @DisplayName("Apple - JWKS에 해당 kid 없음 → AuthException(Apple public key not found for kid)")
    void getSocialUserInfo_apple_kidNotFoundInJwks_throwsAuthException() throws Exception {
        // given
        String targetKid = "missing-kid-xyz";
        String fakeJwt = buildFakeJwt(targetKid);
        String headerJson = "{\"alg\":\"RS256\",\"kid\":\"" + targetKid + "\"}";
        JsonNode headerNode = realMapper.readTree(headerJson);

        // 512비트(64바이트) 이상의 RSA n 값이 필요 - AQAB는 3바이트뿐이라 InvalidKeyException 발생
        // 65바이트짜리 0x00+0xFF*64 를 base64url 인코딩하여 유효한 모듈러스로 사용
        byte[] nBytes = new byte[65]; // leading 0x00 ensures positive BigInteger, then 64 FF bytes
        nBytes[0] = 0x00;
        for (int i = 1; i < 65; i++) nBytes[i] = (byte) 0xFF;
        String validN = Base64.getUrlEncoder().withoutPadding().encodeToString(nBytes);
        // e = 65537 (0x010001)
        String validE = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[]{0x01, 0x00, 0x01});

        // JWKS 응답에는 다른 kid만 포함 (요청한 targetKid 없음)
        String jwksJson = String.format("""
                {
                  "keys": [
                    {
                      "kty": "RSA",
                      "kid": "different-kid-999",
                      "use": "sig",
                      "alg": "RS256",
                      "n": "%s",
                      "e": "%s"
                    }
                  ]
                }
                """, validN, validE);
        JsonNode jwksNode = realMapper.readTree(jwksJson);

        given(objectMapper.readTree(headerJson)).willReturn(headerNode);
        given(restTemplate.getForEntity(anyString(), eq(String.class)))
                .willReturn(new ResponseEntity<>(jwksJson, HttpStatus.OK));
        given(objectMapper.readTree(jwksJson)).willReturn(jwksNode);

        // when & then
        assertThatThrownBy(() -> socialAuthService.getSocialUserInfo("apple", fakeJwt))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Apple public key not found for kid: " + targetKid);
    }

    @Test
    @DisplayName("Apple - provider 문자열 대소문자 무관 처리 (APPLE 대문자) → 동일한 에러 경로")
    void getSocialUserInfo_apple_upperCase_sameFlow() {
        // given - 포맷이 잘못된 토큰으로 대소문자 무관 라우팅 검증
        String malformedToken = "only.two";

        // when & then
        assertThatThrownBy(() -> socialAuthService.getSocialUserInfo("APPLE", malformedToken))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Invalid Apple identity token format");
    }
}
