package com.min.chalkakserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.min.chalkakserver.dto.PagedResponseDto;
import com.min.chalkakserver.dto.favorite.FavoriteResponseDto;
import com.min.chalkakserver.entity.User;
import com.min.chalkakserver.exception.GlobalExceptionHandler;
import com.min.chalkakserver.security.CustomUserDetails;
import com.min.chalkakserver.service.FavoriteService;
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
class FavoriteControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Mock
    private FavoriteService favoriteService;

    @InjectMocks
    private FavoriteController favoriteController;

    private CustomUserDetails userDetails;
    private FavoriteResponseDto sampleFavorite;

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

        mockMvc = MockMvcBuilders.standaloneSetup(favoriteController)
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

        sampleFavorite = FavoriteResponseDto.builder()
                .id(1L)
                .photoBooth(com.min.chalkakserver.dto.PhotoBoothResponseDto.builder().id(10L).name("테스트").build())
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void addFavorite_ShouldReturn200WithFavoriteResponse() throws Exception {
        given(favoriteService.addFavorite(any(), eq(10L))).willReturn(sampleFavorite);

        mockMvc.perform(post("/api/favorites/{photoBoothId}", 10L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.photoBooth.id").value(10L));
    }

    @Test
    void removeFavorite_ShouldReturn200WithMessage() throws Exception {
        doNothing().when(favoriteService).removeFavorite(any(), eq(10L));

        mockMvc.perform(delete("/api/favorites/{photoBoothId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getMyFavorites_ShouldReturnList() throws Exception {
        given(favoriteService.getMyFavorites(any())).willReturn(List.of(sampleFavorite));

        mockMvc.perform(get("/api/favorites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    void getMyFavoritesPaged_ShouldReturnPagedResponse() throws Exception {
        PagedResponseDto<FavoriteResponseDto> pagedResponse = PagedResponseDto.from(
                new PageImpl<>(List.of(sampleFavorite), PageRequest.of(0, 20), 1)
        );

        given(favoriteService.getMyFavoritesPaged(any(), eq(0), eq(20))).willReturn(pagedResponse);

        mockMvc.perform(get("/api/favorites/paged")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void checkFavorite_WhenFavorited_ShouldReturnTrue() throws Exception {
        given(favoriteService.isFavorite(any(), eq(10L))).willReturn(true);

        mockMvc.perform(get("/api/favorites/check/{photoBoothId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isFavorite").value(true));
    }

    @Test
    void checkFavorite_WhenNotFavorited_ShouldReturnFalse() throws Exception {
        given(favoriteService.isFavorite(any(), eq(10L))).willReturn(false);

        mockMvc.perform(get("/api/favorites/check/{photoBoothId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isFavorite").value(false));
    }

    @Test
    void getFavoriteCount_ShouldReturnCount() throws Exception {
        given(favoriteService.getFavoriteCount(eq(10L))).willReturn(42L);

        mockMvc.perform(get("/api/favorites/count/{photoBoothId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(42L));
    }
}
