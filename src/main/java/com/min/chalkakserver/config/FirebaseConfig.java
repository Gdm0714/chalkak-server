package com.min.chalkakserver.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.service-account-path:}")
    private String serviceAccountPath;

    /**
     * FirebaseMessaging 빈은 service-account-path가 유효한 파일을 가리킬 때만 등록한다.
     * (Bean 메서드가 null을 반환하면 Spring DI가 깨지므로, 등록을 아예 하지 않는다.)
     * NotificationService는 @Autowired(required = false)로 받아 null 처리.
     */
    @Bean
    @ConditionalOnExpression("'${firebase.service-account-path:}' != ''")
    public FirebaseMessaging firebaseMessaging() throws IOException {
        try (FileInputStream serviceAccount = new FileInputStream(serviceAccountPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("FirebaseApp initialized successfully.");
            }

            return FirebaseMessaging.getInstance();
        }
    }
}
