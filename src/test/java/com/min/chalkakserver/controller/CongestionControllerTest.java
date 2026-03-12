package com.min.chalkakserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.min.chalkakserver.dto.congestion.CongestionReportRequestDto;
import com.min.chalkakserver.dto.congestion.CongestionReportResponseDto;
import com.min.chalkakserver.dto.congestion.CongestionResponseDto;
import com.min.chalkakserver.entity.CongestionReport;
import com.min.chalkakserver.entity.User;
import com.min.chalkakserver.exception.DuplicateCongestionReportException;
import com.min.chalkakserver.exception.GlobalExceptionHandler;
import com.min.chalkakserver.security.CustomUserDetails;
import com.min.chalkakserver.service.CongestionService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CongestionControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Mock
    private CongestionService congestionService;

    @InjectMocks
    private CongestionController congestionController;

    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() throws Exception {
        User testUser = User.builder()
                .email("test@test.com")
                .nickname("tester")
                .provider(User.AuthProvider.EMAIL)
                .providerId("test@test.com")
                .role(User.Role.USER)
                .build();
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(testUser, 1L);

        userDetails = new CustomUserDetails(testUser);

        mockMvc = MockMvcBuilders.standaloneSetup(congestionController)
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
    }

    @Test
    void getCurrentCongestion_ShouldReturnCongestionResponse() throws Exception {
        CongestionResponseDto response = CongestionResponseDto.builder()
                .congestionLevel(CongestionReport.CongestionLevel.NORMAL)
                .confidenceLevel(CongestionResponseDto.ConfidenceLevel.MEDIUM)
                .sampleSize(5)
                .estimatedWaitMinutesMin(10)
                .estimatedWaitMinutesMax(20)
                .lastUpdatedAt(LocalDateTime.now())
                .message("보통이에요.")
                .build();

        given(congestionService.getCurrentCongestion(eq(10L))).willReturn(response);

        mockMvc.perform(get("/api/congestion/photo-booth/{photoBoothId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.congestionLevel").value("NORMAL"))
                .andExpect(jsonPath("$.sampleSize").value(5))
                .andExpect(jsonPath("$.estimatedWaitMinutesMin").value(10))
                .andExpect(jsonPath("$.estimatedWaitMinutesMax").value(20))
                .andExpect(jsonPath("$.message").value("보통이에요."));
    }

    @Test
    void getCurrentCongestion_NoData_ShouldReturnUnknown() throws Exception {
        CongestionResponseDto response = CongestionResponseDto.builder()
                .congestionLevel(CongestionReport.CongestionLevel.UNKNOWN)
                .confidenceLevel(CongestionResponseDto.ConfidenceLevel.LOW)
                .sampleSize(0)
                .estimatedWaitMinutesMin(0)
                .estimatedWaitMinutesMax(0)
                .lastUpdatedAt(null)
                .message("아직 혼잡도 데이터가 부족해요.")
                .build();

        given(congestionService.getCurrentCongestion(eq(99L))).willReturn(response);

        mockMvc.perform(get("/api/congestion/photo-booth/{photoBoothId}", 99L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.congestionLevel").value("UNKNOWN"))
                .andExpect(jsonPath("$.message").value("아직 혼잡도 데이터가 부족해요."));
    }

    @Test
    void submitCongestionReport_ValidRequest_ShouldReturn200() throws Exception {
        CongestionReportRequestDto request = CongestionReportRequestDto.builder()
                .congestionLevel(CongestionReport.CongestionLevel.NORMAL)
                .latitude(37.5665)
                .longitude(126.9780)
                .build();

        CongestionReportResponseDto response = CongestionReportResponseDto.builder()
                .photoBoothId(10L)
                .message("혼잡도가 제보되었습니다.")
                .submittedAt(LocalDateTime.now())
                .build();

        given(congestionService.submitReport(any(), eq(10L), any())).willReturn(response);

        mockMvc.perform(post("/api/congestion/photo-booth/{photoBoothId}", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoBoothId").value(10L))
                .andExpect(jsonPath("$.message").value("혼잡도가 제보되었습니다."));
    }

    @Test
    void submitCongestionReport_BlankLevel_ShouldReturn400() throws Exception {
        CongestionReportRequestDto invalidRequest = new CongestionReportRequestDto();

        mockMvc.perform(post("/api/congestion/photo-booth/{photoBoothId}", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitCongestionReport_Duplicate_ShouldReturn409() throws Exception {
        CongestionReportRequestDto request = CongestionReportRequestDto.builder()
                .congestionLevel(CongestionReport.CongestionLevel.BUSY)
                .latitude(37.5665)
                .longitude(126.9780)
                .build();

        given(congestionService.submitReport(any(), eq(10L), any()))
                .willThrow(new DuplicateCongestionReportException(10L));

        mockMvc.perform(post("/api/congestion/photo-booth/{photoBoothId}", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}
