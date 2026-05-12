package com.min.chalkakserver.controller;

import com.min.chalkakserver.service.StatsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("StatsController 테스트")
class StatsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private StatsService statsService;

    @BeforeEach
    void setUp() {
        StatsController statsController = new StatsController(statsService);
        mockMvc = MockMvcBuilders.standaloneSetup(statsController).build();
    }

    @Test
    @DisplayName("앱 통계 요약을 반환한다")
    void getSummary_success() throws Exception {
        given(statsService.getSummary()).willReturn(Map.of(
                "photoBoothCount", 12L,
                "activeUserCount", 34L,
                "reviewCount", 56L
        ));

        mockMvc.perform(get("/api/stats/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.photoBoothCount").value(12))
            .andExpect(jsonPath("$.activeUserCount").value(34))
            .andExpect(jsonPath("$.reviewCount").value(56));
    }
}
