package com.min.chalkakserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.min.chalkakserver.config.RateLimitConfig;
import com.min.chalkakserver.config.WebMvcConfig;
import com.min.chalkakserver.dto.PhotoBoothReportDto;
import com.min.chalkakserver.exception.PhotoBoothNotFoundException;
import com.min.chalkakserver.security.jwt.JwtAuthenticationFilter;
import com.min.chalkakserver.service.EmailService;
import com.min.chalkakserver.service.PhotoBoothService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = PhotoBoothController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = WebMvcConfig.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class PhotoBoothControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PhotoBoothService photoBoothService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RateLimitConfig rateLimitConfig;

    @MockBean
    private EmailService emailService;

    @Test
    void getPhotoBoothById_NotFound_ShouldReturn404() throws Exception {
        // Given
        Long nonExistentId = 999L;
        when(photoBoothService.getPhotoBoothById(anyLong()))
                .thenThrow(new PhotoBoothNotFoundException(nonExistentId));

        // When & Then
        mockMvc.perform(get("/api/photo-booths/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.details.photoBoothId").value(nonExistentId));
    }

    @Test
    void reportPhotoBooth_InvalidInput_ShouldReturn400() throws Exception {
        // Given
        PhotoBoothReportDto invalidRequest = new PhotoBoothReportDto();
        // 필수 필드를 비워둠

        // When & Then
        mockMvc.perform(post("/api/photo-booths/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.details.fieldErrors").exists());
    }

    @Test
    void getNearbyPhotoBooths_InvalidCoordinates_ShouldReturn400() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/photo-booths/nearby")
                        .param("latitude", "100") // 유효하지 않은 위도
                        .param("longitude", "200") // 유효하지 않은 경도
                        .param("radius", "3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getAllPhotoBooths_ShouldReturn200() throws Exception {
        when(photoBoothService.getAllPhotoBooths()).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/photo-booths"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getAllPhotoBoothsPaged_ShouldReturn200() throws Exception {
        com.min.chalkakserver.dto.PagedResponseDto<com.min.chalkakserver.dto.PhotoBoothResponseDto> pagedResponse =
                com.min.chalkakserver.dto.PagedResponseDto.of(java.util.List.of());
        when(photoBoothService.getAllPhotoBoothsPaged(0, 20)).thenReturn(pagedResponse);

        mockMvc.perform(get("/api/photo-booths/paged")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getPhotoBoothById_Success_ShouldReturn200() throws Exception {
        com.min.chalkakserver.dto.PhotoBoothResponseDto dto = com.min.chalkakserver.dto.PhotoBoothResponseDto.builder()
                .id(1L)
                .name("테스트 사진관")
                .brand("인생네컷")
                .address("서울시 강남구")
                .build();
        when(photoBoothService.getPhotoBoothById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/photo-booths/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("테스트 사진관"));
    }

    @Test
    void searchPhotoBooths_ShouldReturn200() throws Exception {
        when(photoBoothService.searchPhotoBooths("강남")).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/photo-booths/search")
                        .param("keyword", "강남"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void searchPhotoBoothsPaged_ShouldReturn200() throws Exception {
        com.min.chalkakserver.dto.PagedResponseDto<com.min.chalkakserver.dto.PhotoBoothResponseDto> pagedResponse =
                com.min.chalkakserver.dto.PagedResponseDto.of(java.util.List.of());
        when(photoBoothService.searchPhotoBoothsPaged("강남", 0, 20)).thenReturn(pagedResponse);

        mockMvc.perform(get("/api/photo-booths/search/paged")
                        .param("keyword", "강남")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getPhotoBoothsByBrand_ShouldReturn200() throws Exception {
        when(photoBoothService.getPhotoBoothsByBrand("인생네컷")).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/photo-booths/brand/{brand}", "인생네컷"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getPhotoBoothsByBrandPaged_ShouldReturn200() throws Exception {
        com.min.chalkakserver.dto.PagedResponseDto<com.min.chalkakserver.dto.PhotoBoothResponseDto> pagedResponse =
                com.min.chalkakserver.dto.PagedResponseDto.of(java.util.List.of());
        when(photoBoothService.getPhotoBoothsByBrandPaged("인생네컷", 0, 20)).thenReturn(pagedResponse);

        mockMvc.perform(get("/api/photo-booths/brand/{brand}/paged", "인생네컷")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getPhotoBoothsByBrandAndSeries_ShouldReturn200() throws Exception {
        when(photoBoothService.getPhotoBoothsByBrandAndSeries("인생네컷", "컬러")).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/photo-booths/brand/{brand}/series/{series}", "인생네컷", "컬러"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getPhotoBoothsBySeries_ShouldReturn200() throws Exception {
        when(photoBoothService.getPhotoBoothsBySeries("오리지널")).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/photo-booths/series/{series}", "오리지널"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getNearbyPhotoBooths_ValidCoordinates_ShouldReturn200() throws Exception {
        when(photoBoothService.getNearbyPhotoBooths(37.5, 127.0, 3.0)).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/photo-booths/nearby")
                        .param("latitude", "37.5")
                        .param("longitude", "127.0")
                        .param("radius", "3.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void reportPhotoBooth_ValidInput_ShouldReturn200() throws Exception {
        com.min.chalkakserver.dto.PhotoBoothReportDto reportDto = com.min.chalkakserver.dto.PhotoBoothReportDto.builder()
                .name("새 사진관")
                .address("서울시 강남구")
                .latitude(37.5)
                .longitude(127.0)
                .build();

        mockMvc.perform(post("/api/photo-booths/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("제보가 접수되었습니다. 감사합니다!"));
    }
}
