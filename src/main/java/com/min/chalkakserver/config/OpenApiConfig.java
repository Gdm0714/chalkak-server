package com.min.chalkakserver.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
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
                ));
    }
}
