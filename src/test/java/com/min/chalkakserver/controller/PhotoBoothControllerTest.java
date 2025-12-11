package com.min.chalkakserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.min.chalkakserver.dto.PhotoBoothRequestDto;
import com.min.chalkakserver.exception.PhotoBoothNotFoundException;
import com.min.chalkakserver.service.PhotoBoothService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PhotoBoothController.class)
class PhotoBoothControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PhotoBoothService photoBoothService;

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
    void createPhotoBooth_InvalidInput_ShouldReturn400() throws Exception {
        // Given
        PhotoBoothRequestDto invalidRequest = new PhotoBoothRequestDto();
        // 필수 필드를 비워둠

        // When & Then
        mockMvc.perform(post("/api/photo-booths")
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
}
