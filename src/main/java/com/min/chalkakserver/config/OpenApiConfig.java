package com.min.chalkakserver.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
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
        return new OpenAPI()
                .info(new Info()
                        .title("Chalkak API")
                        .description("네컷사진관 찾기 서비스 API 문서")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Chalkak Team")
                                .email("ehdals45454@gmail.com")))
                .servers(List.of(
                        new Server()
                                .url("https://api.chalkak.co.kr")
                                .description("프로덕션 서버"),
                        new Server()
                                .url("http://localhost:8082")
                                .description("로컬 개발 서버")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT 토큰을 입력하세요. (Bearer 접두사 제외)")
                        )
                );
    }
}
