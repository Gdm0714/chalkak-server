package com.min.chalkakserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.min.chalkakserver.config.RateLimitConfig;
import com.min.chalkakserver.config.WebMvcConfig;
import com.min.chalkakserver.dto.PagedResponseDto;
import com.min.chalkakserver.dto.PhotoBoothRequestDto;
import com.min.chalkakserver.dto.PhotoBoothResponseDto;
import com.min.chalkakserver.dto.admin.AdminStatsDto;
import com.min.chalkakserver.dto.admin.UserListResponseDto;
import com.min.chalkakserver.entity.User;
import com.min.chalkakserver.security.CustomUserDetails;
import com.min.chalkakserver.security.jwt.JwtAuthenticationFilter;
import com.min.chalkakserver.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AdminController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = WebMvcConfig.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminService adminService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RateLimitConfig rateLimitConfig;

    private CustomUserDetails adminUserDetails;
    private PhotoBoothResponseDto samplePhotoBooth;
    private UserListResponseDto sampleUser;
    private AdminStatsDto sampleStats;

    @BeforeEach
    void setUp() throws Exception {
        User adminUser = User.builder()
                .email("admin@test.com")
                .nickname("admin")
                .provider(User.AuthProvider.EMAIL)
                .providerId("admin@test.com")
                .role(User.Role.ADMIN)
                .build();
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(adminUser, 1L);

        adminUserDetails = new CustomUserDetails(adminUser);

        samplePhotoBooth = PhotoBoothResponseDto.builder()
                .id(10L)
                .name("인생네컷 강남점")
                .brand("인생네컷")
                .address("서울 강남구 테헤란로 1")
                .latitude(37.5665)
                .longitude(126.9780)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        sampleUser = UserListResponseDto.builder()
                .id(2L)
                .email("user@test.com")
                .nickname("testuser")
                .provider("EMAIL")
                .role("USER")
                .createdAt(LocalDateTime.now())
                .lastLoginAt(LocalDateTime.now())
                .reviewCount(3)
                .favoriteCount(5)
                .build();

        sampleStats = AdminStatsDto.builder()
                .totalPhotoBooths(100)
                .totalUsers(500)
                .totalReviews(1200)
                .totalFavorites(800)
                .newUsersToday(10)
                .newReviewsToday(25)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getStats_AdminUser_ShouldReturn200() throws Exception {
        given(adminService.getStats()).willReturn(sampleStats);

        mockMvc.perform(get("/api/admin/stats")
                        .with(user(adminUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPhotoBooths").value(100))
                .andExpect(jsonPath("$.totalUsers").value(500))
                .andExpect(jsonPath("$.totalReviews").value(1200))
                .andExpect(jsonPath("$.newUsersToday").value(10));
    }

    @Test
    void createPhotoBooth_ValidRequest_ShouldReturn200() throws Exception {
        PhotoBoothRequestDto request = PhotoBoothRequestDto.builder()
                .name("인생네컷 강남점")
                .address("서울 강남구 테헤란로 1")
                .latitude(37.5665)
                .longitude(126.9780)
                .build();

        given(adminService.createPhotoBooth(any())).willReturn(samplePhotoBooth);

        mockMvc.perform(post("/api/admin/photo-booths")
                        .with(user(adminUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.name").value("인생네컷 강남점"))
                .andExpect(jsonPath("$.brand").value("인생네컷"));
    }

    @Test
    void createPhotoBooth_MissingName_ShouldReturn400() throws Exception {
        PhotoBoothRequestDto request = PhotoBoothRequestDto.builder()
                .address("서울 강남구 테헤란로 1")
                .latitude(37.5665)
                .longitude(126.9780)
                .build();

        mockMvc.perform(post("/api/admin/photo-booths")
                        .with(user(adminUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatePhotoBooth_ValidRequest_ShouldReturn200() throws Exception {
        PhotoBoothRequestDto request = PhotoBoothRequestDto.builder()
                .name("인생네컷 강남점 수정")
                .address("서울 강남구 테헤란로 1")
                .latitude(37.5665)
                .longitude(126.9780)
                .build();

        PhotoBoothResponseDto updated = PhotoBoothResponseDto.builder()
                .id(10L)
                .name("인생네컷 강남점 수정")
                .brand("인생네컷")
                .address("서울 강남구 테헤란로 1")
                .latitude(37.5665)
                .longitude(126.9780)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        given(adminService.updatePhotoBooth(eq(10L), any())).willReturn(updated);

        mockMvc.perform(put("/api/admin/photo-booths/{id}", 10L)
                        .with(user(adminUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.name").value("인생네컷 강남점 수정"));
    }

    @Test
    void deletePhotoBooth_ValidId_ShouldReturn200WithMessage() throws Exception {
        doNothing().when(adminService).deletePhotoBooth(eq(10L));

        mockMvc.perform(delete("/api/admin/photo-booths/{id}", 10L)
                        .with(user(adminUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("포토부스가 삭제되었습니다."));
    }

    @Test
    void getUsers_ShouldReturnPagedResponse() throws Exception {
        PagedResponseDto<UserListResponseDto> pagedResponse = PagedResponseDto.from(
                new PageImpl<>(List.of(sampleUser), PageRequest.of(0, 20), 1)
        );

        given(adminService.getUsers(eq(0), eq(20))).willReturn(pagedResponse);

        mockMvc.perform(get("/api/admin/users")
                        .with(user(adminUserDetails))
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value("user@test.com"));
    }

    @Test
    void getUser_ValidUserId_ShouldReturn200() throws Exception {
        given(adminService.getUser(eq(2L))).willReturn(sampleUser);

        mockMvc.perform(get("/api/admin/users/{userId}", 2L)
                        .with(user(adminUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.email").value("user@test.com"))
                .andExpect(jsonPath("$.reviewCount").value(3))
                .andExpect(jsonPath("$.favoriteCount").value(5));
    }

    @Test
    void updateUserRole_ValidRequest_ShouldReturn200() throws Exception {
        UserListResponseDto updatedUser = UserListResponseDto.builder()
                .id(2L)
                .email("user@test.com")
                .nickname("testuser")
                .provider("EMAIL")
                .role("ADMIN")
                .createdAt(LocalDateTime.now())
                .lastLoginAt(LocalDateTime.now())
                .reviewCount(3)
                .favoriteCount(5)
                .build();

        given(adminService.updateUserRole(eq(2L), eq("ADMIN"))).willReturn(updatedUser);

        mockMvc.perform(patch("/api/admin/users/{userId}/role", 2L)
                        .with(user(adminUserDetails))
                        .param("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void deleteUser_ValidUserId_ShouldReturn200WithMessage() throws Exception {
        doNothing().when(adminService).deleteUser(eq(2L));

        mockMvc.perform(delete("/api/admin/users/{userId}", 2L)
                        .with(user(adminUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("사용자가 삭제되었습니다."));
    }

    @Test
    void deleteReview_ValidReviewId_ShouldReturn200WithMessage() throws Exception {
        doNothing().when(adminService).deleteReview(eq(5L));

        mockMvc.perform(delete("/api/admin/reviews/{reviewId}", 5L)
                        .with(user(adminUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("리뷰가 삭제되었습니다."));
    }
}
