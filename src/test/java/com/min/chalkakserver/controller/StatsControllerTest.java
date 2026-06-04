package com.min.chalkakserver.controller;

import com.min.chalkakserver.config.RateLimitConfig;
import com.min.chalkakserver.config.WebMvcConfig;
import com.min.chalkakserver.dto.stats.AppStatsSummaryDto;
import com.min.chalkakserver.security.jwt.JwtAuthenticationFilter;
import com.min.chalkakserver.service.StatsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = StatsController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = WebMvcConfig.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("StatsController 테스트")
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatsService statsService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RateLimitConfig rateLimitConfig;

    @Test
    @DisplayName("GET /api/stats/summary는 앱에서 기대하는 공개 통계 JSON을 반환한다")
    void getSummary_returnsPublicAppStats() throws Exception {
        AppStatsSummaryDto summary = AppStatsSummaryDto.builder()
                .photoBoothCount(123L)
                .activeUserCount(45L)
                .reviewCount(678L)
                .build();
        given(statsService.getSummary()).willReturn(summary);

        mockMvc.perform(get("/api/stats/summary"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.photoBoothCount").value(123L))
                .andExpect(jsonPath("$.activeUserCount").value(45L))
                .andExpect(jsonPath("$.reviewCount").value(678L));
    }
}
