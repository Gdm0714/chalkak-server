package com.min.chalkakserver.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HealthCheckController 테스트")
class HealthCheckControllerTest {

    @InjectMocks
    private HealthCheckController healthCheckController;

    @Test
    @DisplayName("헬스체크 API가 정상 응답을 반환한다")
    void healthCheck_Success() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(healthCheckController).build();

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.message").value("찰칵 서버가 정상적으로 실행 중입니다!"))
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
