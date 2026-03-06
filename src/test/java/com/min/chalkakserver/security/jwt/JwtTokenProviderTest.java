package com.min.chalkakserver.security.jwt;

import com.min.chalkakserver.entity.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    private static final String VALID_SECRET = "test-secret-key-must-be-at-least-32-characters-long";
    private static final long ACCESS_VALIDITY = 3600000L;   // 1 hour
    private static final long REFRESH_VALIDITY = 1209600000L; // 14 days

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() throws Exception {
        jwtTokenProvider = createProvider(VALID_SECRET, ACCESS_VALIDITY, REFRESH_VALIDITY);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private JwtTokenProvider createProvider(String secret, long accessValidity, long refreshValidity)
            throws Exception {
        JwtTokenProvider provider = new JwtTokenProvider();

        Field secretField = JwtTokenProvider.class.getDeclaredField("secretKey");
        secretField.setAccessible(true);
        secretField.set(provider, secret);

        Field accessField = JwtTokenProvider.class.getDeclaredField("accessTokenValidity");
        accessField.setAccessible(true);
        accessField.set(provider, accessValidity);

        Field refreshField = JwtTokenProvider.class.getDeclaredField("refreshTokenValidity");
        refreshField.setAccessible(true);
        refreshField.set(provider, refreshValidity);

        Method initMethod = JwtTokenProvider.class.getDeclaredMethod("init");
        initMethod.setAccessible(true);
        initMethod.invoke(provider);

        return provider;
    }

    private User buildUser() throws Exception {
        User user = User.builder()
                .email("test@test.com")
                .nickname("tester")
                .provider(User.AuthProvider.EMAIL)
                .providerId("test@test.com")
                .role(User.Role.USER)
                .build();

        // id is auto-generated; set via reflection
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, 42L);

        return user;
    }

    // ── createAccessToken ─────────────────────────────────────────────────────

    @Test
    @DisplayName("createAccessToken - valid JWT with correct claims")
    void createAccessToken_containsCorrectClaims() throws Exception {
        User user = buildUser();

        String token = jwtTokenProvider.createAccessToken(user);

        assertThat(token).isNotBlank();

        Claims claims = jwtTokenProvider.getClaimsFromToken(token);
        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("email", String.class)).isEqualTo("test@test.com");
        assertThat(claims.get("nickname", String.class)).isEqualTo("tester");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
        assertThat(claims.get("provider", String.class)).isEqualTo("EMAIL");
        assertThat(claims.get("type", String.class)).isNull();
    }

    // ── createRefreshToken ────────────────────────────────────────────────────

    @Test
    @DisplayName("createRefreshToken - valid JWT with type=refresh")
    void createRefreshToken_hasRefreshType() throws Exception {
        User user = buildUser();

        String token = jwtTokenProvider.createRefreshToken(user);

        assertThat(token).isNotBlank();

        Claims claims = jwtTokenProvider.getClaimsFromToken(token);
        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("type", String.class)).isEqualTo("refresh");
    }

    // ── validateToken ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("validateToken - valid token returns true")
    void validateToken_validToken_returnsTrue() throws Exception {
        User user = buildUser();
        String token = jwtTokenProvider.createAccessToken(user);

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("validateToken - expired token returns false")
    void validateToken_expiredToken_returnsFalse() throws Exception {
        // create provider with 1 ms validity so token expires immediately
        JwtTokenProvider shortLivedProvider = createProvider(VALID_SECRET, 1L, 1L);
        User user = buildUser();
        String token = shortLivedProvider.createAccessToken(user);

        // sleep just enough for expiry
        Thread.sleep(10);

        assertThat(shortLivedProvider.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("validateToken - malformed token returns false")
    void validateToken_malformedToken_returnsFalse() {
        assertThat(jwtTokenProvider.validateToken("this.is.not.a.valid.jwt")).isFalse();
    }

    @Test
    @DisplayName("validateToken - null token returns false")
    void validateToken_nullToken_returnsFalse() {
        assertThat(jwtTokenProvider.validateToken(null)).isFalse();
    }

    @Test
    @DisplayName("validateToken - empty token returns false")
    void validateToken_emptyToken_returnsFalse() {
        assertThat(jwtTokenProvider.validateToken("")).isFalse();
    }

    // ── getUserIdFromToken ────────────────────────────────────────────────────

    @Test
    @DisplayName("getUserIdFromToken - extracts correct userId")
    void getUserIdFromToken_returnsCorrectId() throws Exception {
        User user = buildUser();
        String token = jwtTokenProvider.createAccessToken(user);

        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        assertThat(userId).isEqualTo(42L);
    }

    // ── getClaimsFromToken ────────────────────────────────────────────────────

    @Test
    @DisplayName("getClaimsFromToken - returns all claims correctly")
    void getClaimsFromToken_returnsAllClaims() throws Exception {
        User user = buildUser();
        String token = jwtTokenProvider.createAccessToken(user);

        Claims claims = jwtTokenProvider.getClaimsFromToken(token);

        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("email", String.class)).isEqualTo("test@test.com");
        assertThat(claims.get("nickname", String.class)).isEqualTo("tester");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
        assertThat(claims.get("provider", String.class)).isEqualTo("EMAIL");
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    // ── getAccessTokenValidity / getRefreshTokenValidity ──────────────────────

    @Test
    @DisplayName("getAccessTokenValidity - returns configured value")
    void getAccessTokenValidity_returnsConfiguredValue() {
        assertThat(jwtTokenProvider.getAccessTokenValidity()).isEqualTo(ACCESS_VALIDITY);
    }

    @Test
    @DisplayName("getRefreshTokenValidity - returns configured value")
    void getRefreshTokenValidity_returnsConfiguredValue() {
        assertThat(jwtTokenProvider.getRefreshTokenValidity()).isEqualTo(REFRESH_VALIDITY);
    }

    // ── getRefreshTokenExpiryDate ─────────────────────────────────────────────

    @Test
    @DisplayName("getRefreshTokenExpiryDate - returns a future date")
    void getRefreshTokenExpiryDate_returnsFutureDate() {
        Date before = new Date();
        Date expiry = jwtTokenProvider.getRefreshTokenExpiryDate();
        Date after = new Date(System.currentTimeMillis() + REFRESH_VALIDITY + 1000);

        assertThat(expiry).isAfter(before);
        assertThat(expiry).isBefore(after);
    }

    // ── init validation ───────────────────────────────────────────────────────

    @Test
    @DisplayName("init - blank secret throws IllegalStateException")
    void init_blankSecret_throwsIllegalStateException() throws Exception {
        JwtTokenProvider provider = new JwtTokenProvider();

        Field secretField = JwtTokenProvider.class.getDeclaredField("secretKey");
        secretField.setAccessible(true);
        secretField.set(provider, "   ");

        Field accessField = JwtTokenProvider.class.getDeclaredField("accessTokenValidity");
        accessField.setAccessible(true);
        accessField.set(provider, ACCESS_VALIDITY);

        Field refreshField = JwtTokenProvider.class.getDeclaredField("refreshTokenValidity");
        refreshField.setAccessible(true);
        refreshField.set(provider, REFRESH_VALIDITY);

        Method initMethod = JwtTokenProvider.class.getDeclaredMethod("init");
        initMethod.setAccessible(true);

        assertThatThrownBy(() -> {
            try {
                initMethod.invoke(provider);
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw e.getCause();
            }
        }).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("init - short secret (< 32 chars) throws IllegalStateException")
    void init_shortSecret_throwsIllegalStateException() throws Exception {
        JwtTokenProvider provider = new JwtTokenProvider();

        Field secretField = JwtTokenProvider.class.getDeclaredField("secretKey");
        secretField.setAccessible(true);
        secretField.set(provider, "tooshort");

        Field accessField = JwtTokenProvider.class.getDeclaredField("accessTokenValidity");
        accessField.setAccessible(true);
        accessField.set(provider, ACCESS_VALIDITY);

        Field refreshField = JwtTokenProvider.class.getDeclaredField("refreshTokenValidity");
        refreshField.setAccessible(true);
        refreshField.set(provider, REFRESH_VALIDITY);

        Method initMethod = JwtTokenProvider.class.getDeclaredMethod("init");
        initMethod.setAccessible(true);

        assertThatThrownBy(() -> {
            try {
                initMethod.invoke(provider);
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw e.getCause();
            }
        }).isInstanceOf(IllegalStateException.class);
    }
}
