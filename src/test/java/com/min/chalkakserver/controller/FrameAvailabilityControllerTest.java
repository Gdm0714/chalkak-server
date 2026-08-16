package com.min.chalkakserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.min.chalkakserver.dto.frameavailability.FrameAvailabilityReportRequestDto;
import com.min.chalkakserver.dto.frameavailability.FrameAvailabilityReportResponseDto;
import com.min.chalkakserver.dto.frameavailability.FrameAvailabilityResponseDto;
import com.min.chalkakserver.entity.FrameAvailabilityReport;
import com.min.chalkakserver.entity.User;
import com.min.chalkakserver.exception.DuplicateAvailabilityReportException;
import com.min.chalkakserver.exception.GlobalExceptionHandler;
import com.min.chalkakserver.security.CustomUserDetails;
import com.min.chalkakserver.service.FrameAvailabilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("FrameAvailabilityController 테스트")
class FrameAvailabilityControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Mock
    private FrameAvailabilityService frameAvailabilityService;

    @InjectMocks
    private FrameAvailabilityController frameAvailabilityController;

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

        mockMvc = MockMvcBuilders.standaloneSetup(frameAvailabilityController)
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
    @DisplayName("GET /collab/{id}/booth/{id} - 매장 재고 상태를 반환한다")
    void getAvailability() throws Exception {
        given(frameAvailabilityService.getAvailability(1L, 10L))
                .willReturn(FrameAvailabilityResponseDto.builder()
                        .frameCollabId(1L)
                        .photoBoothId(10L)
                        .status(FrameAvailabilityResponseDto.ResultStatus.AVAILABLE)
                        .confidenceLevel(FrameAvailabilityResponseDto.ConfidenceLevel.HIGH)
                        .sampleSize(7)
                        .build());

        mockMvc.perform(get("/api/frame-availability/collab/1/booth/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.confidenceLevel").value("HIGH"))
                .andExpect(jsonPath("$.sampleSize").value(7));
    }

    @Test
    @DisplayName("GET /collab/{id} - 콜라보 전체 매장 재고 목록을 반환한다")
    void getCollabAvailability() throws Exception {
        given(frameAvailabilityService.getCollabAvailability(1L))
                .willReturn(List.of(
                        FrameAvailabilityResponseDto.builder()
                                .frameCollabId(1L)
                                .photoBoothId(10L)
                                .status(FrameAvailabilityResponseDto.ResultStatus.SOLD_OUT)
                                .confidenceLevel(FrameAvailabilityResponseDto.ConfidenceLevel.LOW)
                                .sampleSize(1)
                                .build()));

        mockMvc.perform(get("/api/frame-availability/collab/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].photoBoothId").value(10))
                .andExpect(jsonPath("$[0].status").value("SOLD_OUT"));
    }

    @Test
    @DisplayName("POST /collab/{id}/booth/{id} - 재고 제보를 접수한다")
    void submitReport() throws Exception {
        given(frameAvailabilityService.submitReport(eq(1L), eq(1L), eq(10L), any()))
                .willReturn(FrameAvailabilityReportResponseDto.builder()
                        .frameCollabId(1L)
                        .photoBoothId(10L)
                        .message("재고 제보가 반영되었습니다.")
                        .build());

        FrameAvailabilityReportRequestDto request = FrameAvailabilityReportRequestDto.builder()
                .availabilityStatus(FrameAvailabilityReport.AvailabilityStatus.SOLD_OUT)
                .build();

        mockMvc.perform(post("/api/frame-availability/collab/1/booth/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("재고 제보가 반영되었습니다."));
    }

    @Test
    @DisplayName("POST - 쿨다운 중복 제보는 409를 반환한다")
    void submitReport_Duplicate() throws Exception {
        given(frameAvailabilityService.submitReport(eq(1L), eq(1L), eq(10L), any()))
                .willThrow(new DuplicateAvailabilityReportException(1L, 10L));

        FrameAvailabilityReportRequestDto request = FrameAvailabilityReportRequestDto.builder()
                .availabilityStatus(FrameAvailabilityReport.AvailabilityStatus.AVAILABLE)
                .build();

        mockMvc.perform(post("/api/frame-availability/collab/1/booth/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.details.frameCollabId").value(1))
                .andExpect(jsonPath("$.details.photoBoothId").value(10));
    }
}
