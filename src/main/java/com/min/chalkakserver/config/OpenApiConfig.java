package com.min.chalkakserver.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@Slf4j
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        log.info("OpenApiConfig openAPI Bean is being created");

        String description = """
                ## 찰칵 API 문서

                네컷사진관 위치 정보를 제공하는 RESTful API 서비스입니다.

                ### 주요 기능
                - 📍 **위치 기반 검색**: 현재 위치에서 가까운 네컷사진관 찾기
                - 🏷️ **브랜드 필터링**: 인생네컷, 포토이즘, 하루필름 등 브랜드별 검색
                - 🔍 **키워드 검색**: 이름, 주소 기반 검색
                - ⭐ **즐겨찾기**: 자주 가는 포토부스 저장
                - 📝 **리뷰**: 포토부스에 대한 리뷰 작성 및 조회
                - 📧 **제보**: 새로운 포토부스 정보 제보

                ### 인증 방법
                JWT 토큰을 사용한 Bearer 인증을 지원합니다.

                1. `/api/auth/login` 또는 `/api/auth/register`로 로그인/회원가입
                2. 받은 `accessToken`을 우측 상단 🔒 Authorize 버튼에 입력
                3. 인증이 필요한 API 호출 시 자동으로 토큰이 포함됩니다

                ### API 응답 형식
                모든 API는 일관된 형식으로 응답합니다:
                - **성공**: `{ "data": {...}, "message": "success" }`
                - **에러**: `{ "error": "...", "message": "...", "timestamp": "..." }`
                """;

        return new OpenAPI()
                .info(new Info()
                        .title("🎞️ 찰칵 API")
                        .description(description)
                        .version("2.0.0")
                        .contact(new Contact()
                                .name("Chalkak Team")
                                .email("ehdals45454@gmail.com")
                                .url("https://chalkak.co.kr"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .externalDocs(new ExternalDocumentation()
                        .description("GitHub Repository")
                        .url("https://github.com/your-repo/chalkak"))
                .servers(List.of(
                        new Server()
                                .url("https://api.chalkak.co.kr")
                                .description("🚀 프로덕션 서버"),
                        new Server()
                                .url("http://localhost:8082")
                                .description("💻 로컬 개발 서버")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")
                                .description("""
                                        JWT 인증 토큰을 입력하세요.

                                        **중요**: `Bearer` 접두사는 입력하지 마세요. 토큰만 입력하면 됩니다.

                                        예시: `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`
                                        """)
                        )
                );
    }
}
