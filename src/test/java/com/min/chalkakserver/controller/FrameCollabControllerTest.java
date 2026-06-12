package com.min.chalkakserver.controller;

import com.min.chalkakserver.dto.PagedResponseDto;
import com.min.chalkakserver.dto.framecollab.FrameCollabDetailResponseDto;
import com.min.chalkakserver.dto.framecollab.FrameCollabResponseDto;
import com.min.chalkakserver.entity.FrameCollab;
import com.min.chalkakserver.exception.FrameCollabNotFoundException;
import com.min.chalkakserver.exception.GlobalExceptionHandler;
import com.min.chalkakserver.service.FrameCollabService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("FrameCollabController 테스트")
class FrameCollabControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Mock
    private FrameCollabService frameCollabService;

    @InjectMocks
    private FrameCollabController frameCollabController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(frameCollabController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    private FrameCollabResponseDto summaryDto() {
        return FrameCollabResponseDto.builder()
                .id(1L)
                .title("뉴진스 X 인생네컷")
                .brand("인생네컷")
                .artistName("뉴진스")
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .status(FrameCollab.CollabStatus.ACTIVE)
                .photoBoothCount(3)
                .build();
    }

    @Test
    @DisplayName("GET /api/frame-collabs - 콜라보 피드를 반환한다")
    void getCollabs() throws Exception {
        given(frameCollabService.getCollabs(eq("active"), anyInt(), anyInt()))
                .willReturn(PagedResponseDto.of(List.of(summaryDto())));

        mockMvc.perform(get("/api/frame-collabs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("뉴진스 X 인생네컷"))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.content[0].photoBoothCount").value(3));
    }

    @Test
    @DisplayName("GET /api/frame-collabs/{id} - 콜라보 상세를 반환한다")
    void getCollab() throws Exception {
        FrameCollabDetailResponseDto detail = FrameCollabDetailResponseDto.builder()
                .id(1L)
                .title("뉴진스 X 인생네컷")
                .status(FrameCollab.CollabStatus.ACTIVE)
                .photoBooths(List.of())
                .build();
        given(frameCollabService.getCollab(1L)).willReturn(detail);

        mockMvc.perform(get("/api/frame-collabs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.photoBooths").isArray());
    }

    @Test
    @DisplayName("GET /api/frame-collabs/{id} - 없는 콜라보는 404를 반환한다")
    void getCollab_NotFound() throws Exception {
        given(frameCollabService.getCollab(99L)).willThrow(new FrameCollabNotFoundException(99L));

        mockMvc.perform(get("/api/frame-collabs/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.details.frameCollabId").value(99));
    }

    @Test
    @DisplayName("GET /api/frame-collabs/photo-booth/{id} - 매장별 콜라보 목록을 반환한다")
    void getCollabsByPhotoBooth() throws Exception {
        given(frameCollabService.getCollabsByPhotoBooth(10L)).willReturn(List.of(summaryDto()));

        mockMvc.perform(get("/api/frame-collabs/photo-booth/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }
}
