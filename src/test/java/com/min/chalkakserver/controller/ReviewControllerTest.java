package com.min.chalkakserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.min.chalkakserver.dto.PagedResponseDto;
import com.min.chalkakserver.dto.review.ReviewRequestDto;
import com.min.chalkakserver.dto.review.ReviewResponseDto;
import com.min.chalkakserver.dto.review.ReviewStatsDto;
import com.min.chalkakserver.entity.User;
import com.min.chalkakserver.exception.GlobalExceptionHandler;
import com.min.chalkakserver.exception.ReviewNotFoundException;
import com.min.chalkakserver.security.CustomUserDetails;
import com.min.chalkakserver.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ReviewController reviewController;

    private CustomUserDetails userDetails;
    private ReviewResponseDto sampleReview;

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

        mockMvc = MockMvcBuilders.standaloneSetup(reviewController)
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

        sampleReview = ReviewResponseDto.builder()
                .id(1L)
                .photoBoothId(10L)
                .reviewer(ReviewResponseDto.ReviewerDto.builder().id(1L).nickname("tester").build())
                .rating(4)
                .content("좋은 사진관이에요!")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createReview_ValidRequest_ShouldReturn200() throws Exception {
        ReviewRequestDto request = ReviewRequestDto.builder()
                .rating(4)
                .content("좋은 사진관이에요!")
                .build();

        given(reviewService.createReview(any(), eq(10L), any())).willReturn(sampleReview);

        mockMvc.perform(post("/api/reviews/photo-booth/{photoBoothId}", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.content").value("좋은 사진관이에요!"));
    }

    @Test
    void updateReview_ValidRequest_ShouldReturn200() throws Exception {
        ReviewRequestDto request = ReviewRequestDto.builder()
                .rating(5)
                .content("수정된 내용입니다.")
                .build();

        ReviewResponseDto updated = ReviewResponseDto.builder()
                .id(1L)
                .photoBoothId(10L)
                .reviewer(ReviewResponseDto.ReviewerDto.builder().id(1L).nickname("tester").build())
                .rating(5)
                .content("수정된 내용입니다.")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        given(reviewService.updateReview(any(), eq(1L), any())).willReturn(updated);

        mockMvc.perform(put("/api/reviews/{reviewId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.content").value("수정된 내용입니다."));
    }

    @Test
    void deleteReview_ValidRequest_ShouldReturn200WithMessage() throws Exception {
        doNothing().when(reviewService).deleteReview(any(), eq(1L));

        mockMvc.perform(delete("/api/reviews/{reviewId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("리뷰가 삭제되었습니다."));
    }

    @Test
    void getPhotoBoothReviews_ShouldReturnList() throws Exception {
        given(reviewService.getPhotoBoothReviews(eq(10L))).willReturn(List.of(sampleReview));

        mockMvc.perform(get("/api/reviews/photo-booth/{photoBoothId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    void getPhotoBoothReviewsPaged_ShouldReturnPagedResponse() throws Exception {
        PagedResponseDto<ReviewResponseDto> pagedResponse = PagedResponseDto.from(
                new PageImpl<>(List.of(sampleReview), PageRequest.of(0, 20), 1)
        );

        given(reviewService.getPhotoBoothReviewsPaged(eq(10L), eq(0), eq(20))).willReturn(pagedResponse);

        mockMvc.perform(get("/api/reviews/photo-booth/{photoBoothId}/paged", 10L)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getReviewStats_ShouldReturnStats() throws Exception {
        ReviewStatsDto stats = ReviewStatsDto.builder()
                .photoBoothId(10L)
                .averageRating(4.2)
                .totalCount(15L)
                .build();

        given(reviewService.getReviewStats(eq(10L))).willReturn(stats);

        mockMvc.perform(get("/api/reviews/photo-booth/{photoBoothId}/stats", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoBoothId").value(10L))
                .andExpect(jsonPath("$.averageRating").value(4.2))
                .andExpect(jsonPath("$.totalCount").value(15));
    }

    @Test
    void getMyReviews_ShouldReturnUserReviews() throws Exception {
        given(reviewService.getMyReviews(any())).willReturn(List.of(sampleReview));

        mockMvc.perform(get("/api/reviews/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].reviewer.id").value(1L));
    }

    @Test
    void getReview_NotFound_ShouldReturn404() throws Exception {
        given(reviewService.getReview(eq(999L))).willThrow(new ReviewNotFoundException(999L));

        mockMvc.perform(get("/api/reviews/{reviewId}", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMyReviewForPhotoBooth_ShouldReturnReview() throws Exception {
        given(reviewService.getMyReviewForPhotoBooth(any(), eq(10L))).willReturn(sampleReview);

        mockMvc.perform(get("/api/reviews/my/photo-booth/{photoBoothId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoBoothId").value(10L));
    }
}
