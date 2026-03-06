package com.min.chalkakserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.min.chalkakserver.dto.auth.*;
import com.min.chalkakserver.entity.User;
import com.min.chalkakserver.exception.GlobalExceptionHandler;
import com.min.chalkakserver.security.CustomUserDetails;
import com.min.chalkakserver.service.AuthService;
import com.min.chalkakserver.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Mock
    private AuthService authService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthController authController;

    private CustomUserDetails userDetails;
    private AuthResponseDto sampleAuthResponse;
    private UserResponseDto sampleUserResponse;

    @BeforeEach
    void setUp() throws Exception {
        User testUser = User.builder()
                .email("test@test.com")
                .nickname("tester")
                .provider(User.AuthProvider.EMAIL)
                .providerId("test@test.com")
                .role(User.Role.ADMIN)
                .build();
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(testUser, 1L);

        userDetails = new CustomUserDetails(testUser);

        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType().equals(CustomUserDetails.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter,
                            ModelAndViewContainer mavContainer,
                            NativeWebRequest webRequest,
                            WebDataBinderFactory binderFactory) {
                        return userDetails;
                    }
                })
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

        sampleUserResponse = UserResponseDto.builder()
                .id(1L)
                .email("test@test.com")
                .nickname("tester")
                .provider("EMAIL")
                .role("ADMIN")
                .createdAt(LocalDateTime.now())
                .lastLoginAt(LocalDateTime.now())
                .build();

        sampleAuthResponse = AuthResponseDto.builder()
                .accessToken("test-access-token")
                .refreshToken("test-refresh-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .user(sampleUserResponse)
                .build();
    }

    @Test
    void socialLogin_ValidRequest_ShouldReturn200() throws Exception {
        SocialLoginRequestDto request = SocialLoginRequestDto.builder()
                .provider("kakao")
                .accessToken("kakao-access-token")
                .build();

        given(authService.socialLogin(any())).willReturn(sampleAuthResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test-access-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("test@test.com"));
    }

    @Test
    void socialLogin_MissingProvider_ShouldReturn400() throws Exception {
        SocialLoginRequestDto request = SocialLoginRequestDto.builder()
                .accessToken("kakao-access-token")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerWithEmail_ValidRequest_ShouldReturn200() throws Exception {
        EmailRegisterRequestDto request = EmailRegisterRequestDto.builder()
                .email("newuser@test.com")
                .password("password123")
                .termsAgreed(true)
                .privacyAgreed(true)
                .build();

        given(authService.registerWithEmail(any())).willReturn(sampleAuthResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test-access-token"))
                .andExpect(jsonPath("$.user.email").value("test@test.com"));
    }

    @Test
    void registerWithEmail_InvalidEmail_ShouldReturn400() throws Exception {
        EmailRegisterRequestDto request = EmailRegisterRequestDto.builder()
                .email("not-an-email")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginWithEmail_ValidRequest_ShouldReturn200() throws Exception {
        EmailLoginRequestDto request = EmailLoginRequestDto.builder()
                .email("test@test.com")
                .password("password123")
                .build();

        given(authService.loginWithEmail(any())).willReturn(sampleAuthResponse);

        mockMvc.perform(post("/api/auth/login/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("test-refresh-token"));
    }

    @Test
    void refreshToken_ValidRequest_ShouldReturn200() throws Exception {
        RefreshTokenRequestDto request = RefreshTokenRequestDto.builder()
                .refreshToken("valid-refresh-token")
                .build();

        given(authService.refreshToken(any())).willReturn(sampleAuthResponse);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test-access-token"));
    }

    @Test
    void logout_ValidRequest_ShouldReturn200WithMessage() throws Exception {
        RefreshTokenRequestDto request = RefreshTokenRequestDto.builder()
                .refreshToken("valid-refresh-token")
                .build();

        doNothing().when(authService).logout(any());

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("로그아웃 되었습니다."));
    }

    @Test
    void logoutAll_AuthenticatedUser_ShouldReturn200WithMessage() throws Exception {
        doNothing().when(authService).logoutAll(anyLong());

        mockMvc.perform(post("/api/auth/logout-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("모든 기기에서 로그아웃 되었습니다."));
    }

    @Test
    void getCurrentUser_AuthenticatedUser_ShouldReturn200() throws Exception {
        given(authService.getCurrentUser(anyLong())).willReturn(sampleUserResponse);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("test@test.com"))
                .andExpect(jsonPath("$.nickname").value("tester"));
    }

    @Test
    void deleteAccount_AuthenticatedUser_ShouldReturn200WithMessage() throws Exception {
        doNothing().when(authService).deleteAccount(anyLong());

        mockMvc.perform(delete("/api/auth/withdraw"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("회원 탈퇴가 완료되었습니다."));
    }

    @Test
    void updateProfile_ValidRequest_ShouldReturn200() throws Exception {
        ProfileUpdateRequestDto request = ProfileUpdateRequestDto.builder()
                .nickname("updated-nickname")
                .build();

        UserResponseDto updatedUser = UserResponseDto.builder()
                .id(1L)
                .email("test@test.com")
                .nickname("updated-nickname")
                .provider("EMAIL")
                .role("ADMIN")
                .createdAt(LocalDateTime.now())
                .lastLoginAt(LocalDateTime.now())
                .build();

        given(authService.updateProfile(anyLong(), any())).willReturn(updatedUser);

        mockMvc.perform(put("/api/auth/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("updated-nickname"));
    }
}
