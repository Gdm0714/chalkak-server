package com.min.chalkakserver.security.jwt;

import com.min.chalkakserver.entity.User;
import com.min.chalkakserver.security.CustomUserDetails;
import com.min.chalkakserver.service.AuthService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthService authService;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private CustomUserDetails buildCustomUserDetails() throws Exception {
        User user = User.builder()
                .email("test@test.com")
                .nickname("tester")
                .provider(User.AuthProvider.EMAIL)
                .providerId("test@test.com")
                .role(User.Role.USER)
                .build();

        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, 1L);

        return new CustomUserDetails(user);
    }

    // ── no Authorization header ───────────────────────────────────────────────

    @Test
    @DisplayName("No Authorization header - filterChain.doFilter called, no authentication set")
    void noAuthorizationHeader_noAuthenticationSet() throws Exception {
        // no header added
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // ── valid access token ────────────────────────────────────────────────────

    @Test
    @DisplayName("Valid access token - authentication set in SecurityContext")
    void validAccessToken_authenticationSetInSecurityContext() throws Exception {
        String token = "valid-token";
        request.addHeader("Authorization", "Bearer " + token);

        Claims claims = mock(Claims.class);
        given(claims.get("type", String.class)).willReturn(null);

        given(jwtTokenProvider.validateToken(token)).willReturn(true);
        given(jwtTokenProvider.getClaimsFromToken(token)).willReturn(claims);
        given(jwtTokenProvider.getUserIdFromToken(token)).willReturn(1L);

        CustomUserDetails userDetails = buildCustomUserDetails();
        given(authService.loadUserById(1L)).willReturn(userDetails);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isEqualTo(userDetails);
    }

    // ── refresh token rejected ────────────────────────────────────────────────

    @Test
    @DisplayName("Refresh token rejected - no authentication set")
    void refreshToken_noAuthenticationSet() throws Exception {
        String token = "refresh-token";
        request.addHeader("Authorization", "Bearer " + token);

        Claims claims = mock(Claims.class);
        given(claims.get("type", String.class)).willReturn("refresh");

        given(jwtTokenProvider.validateToken(token)).willReturn(true);
        given(jwtTokenProvider.getClaimsFromToken(token)).willReturn(claims);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        // authService must not be called
        verifyNoInteractions(authService);
    }

    // ── invalid token ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Invalid token - validateToken returns false, no auth set, filterChain still called")
    void invalidToken_noAuthSetFilterChainCalled() throws Exception {
        String token = "invalid-token";
        request.addHeader("Authorization", "Bearer " + token);

        given(jwtTokenProvider.validateToken(token)).willReturn(false);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // ── exception during processing ───────────────────────────────────────────

    @Test
    @DisplayName("Exception during processing - filterChain still called")
    void exceptionDuringProcessing_filterChainStillCalled() throws Exception {
        String token = "exception-token";
        request.addHeader("Authorization", "Bearer " + token);

        given(jwtTokenProvider.validateToken(token)).willThrow(new RuntimeException("unexpected error"));

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // ── Bearer prefix missing ─────────────────────────────────────────────────

    @Test
    @DisplayName("Bearer prefix missing - no auth set")
    void bearerPrefixMissing_noAuthSet() throws Exception {
        request.addHeader("Authorization", "Token some-token");

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtTokenProvider);
    }

    // ── empty bearer token ────────────────────────────────────────────────────

    @Test
    @DisplayName("Empty bearer token - no auth set")
    void emptyBearerToken_noAuthSet() throws Exception {
        request.addHeader("Authorization", "Bearer ");

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtTokenProvider);
    }
}
