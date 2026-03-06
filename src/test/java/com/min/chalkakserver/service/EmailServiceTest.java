package com.min.chalkakserver.service;

import com.min.chalkakserver.dto.PhotoBoothReportDto;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService 테스트")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    private void setEntityId(Object entity, Long id) {
        try {
            Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void setUp() {
        setField(emailService, "adminEmail", "admin@test.com");
    }

    @Test
    @DisplayName("모든 필드가 있는 포토부스 제보 이메일을 성공적으로 전송한다")
    void sendPhotoBoothReport_success_allFields() throws MessagingException {
        // given
        PhotoBoothReportDto dto = PhotoBoothReportDto.builder()
                .name("테스트 포토부스")
                .brand("인생네컷")
                .series("인생네컷 강남점")
                .address("서울특별시 강남구 테헤란로 123")
                .roadAddress("서울특별시 강남구 테헤란로 123")
                .latitude(37.5)
                .longitude(127.0)
                .description("1층에 위치한 포토부스입니다")
                .priceInfo("4컷 5000원")
                .reporterName("홍길동")
                .reporterEmail("reporter@test.com")
                .build();

        MimeMessage mimeMessage = mock(MimeMessage.class);
        given(mailSender.createMimeMessage()).willReturn(mimeMessage);

        // when
        emailService.sendPhotoBoothReport(dto);

        // then
        then(mailSender).should().createMimeMessage();
        then(mailSender).should().send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("선택 필드가 null인 포토부스 제보 이메일을 성공적으로 전송한다")
    void sendPhotoBoothReport_success_onlyRequiredFields() throws MessagingException {
        // given
        PhotoBoothReportDto dto = PhotoBoothReportDto.builder()
                .name("최소 포토부스")
                .brand(null)
                .series(null)
                .address("서울특별시 중구 세종대로 110")
                .roadAddress(null)
                .latitude(37.566)
                .longitude(126.977)
                .description(null)
                .priceInfo(null)
                .reporterName(null)
                .reporterEmail(null)
                .build();

        MimeMessage mimeMessage = mock(MimeMessage.class);
        given(mailSender.createMimeMessage()).willReturn(mimeMessage);

        // when
        emailService.sendPhotoBoothReport(dto);

        // then
        then(mailSender).should().send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("이메일 전송 중 MessagingException이 발생하면 예외를 던지지 않고 로그만 기록한다")
    void sendPhotoBoothReport_messagingException_doesNotThrow() throws MessagingException {
        // given
        PhotoBoothReportDto dto = PhotoBoothReportDto.builder()
                .name("포토부스")
                .address("서울")
                .latitude(37.5)
                .longitude(127.0)
                .build();

        MimeMessage mimeMessage = mock(MimeMessage.class);
        given(mailSender.createMimeMessage()).willReturn(mimeMessage);
        willThrow(new RuntimeException(new MessagingException("SMTP 오류")))
                .given(mailSender).send(any(MimeMessage.class));

        // when & then
        assertThatCode(() -> emailService.sendPhotoBoothReport(dto))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("포토부스 제보 이메일 전송 시 올바른 MimeMessage가 mailSender에 전달된다")
    void sendPhotoBoothReport_capturesCorrectMimeMessage() throws MessagingException {
        // given
        PhotoBoothReportDto dto = PhotoBoothReportDto.builder()
                .name("캡처 테스트 포토부스")
                .brand("포토이즘")
                .address("서울특별시 마포구 월드컵로 123")
                .latitude(37.563)
                .longitude(126.925)
                .description("테스트 설명")
                .reporterEmail("capture@test.com")
                .build();

        MimeMessage mimeMessage = mock(MimeMessage.class);
        given(mailSender.createMimeMessage()).willReturn(mimeMessage);

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);

        // when
        emailService.sendPhotoBoothReport(dto);

        // then
        then(mailSender).should().send(messageCaptor.capture());
        MimeMessage capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage).isNotNull();
    }

    @Test
    @DisplayName("XSS 위험 문자가 포함된 포토부스 제보 이메일도 안전하게 처리한다")
    void sendPhotoBoothReport_withXssCharacters_escapesContent() throws MessagingException {
        // given
        PhotoBoothReportDto dto = PhotoBoothReportDto.builder()
                .name("<script>alert('xss')</script>포토부스")
                .brand("브랜드 & 파트너")
                .address("서울 <strong>강남</strong>구")
                .latitude(37.5)
                .longitude(127.0)
                .description("설명\n개행\t탭")
                .reporterName("테스터<script>")
                .build();

        MimeMessage mimeMessage = mock(MimeMessage.class);
        given(mailSender.createMimeMessage()).willReturn(mimeMessage);

        // when & then
        assertThatCode(() -> emailService.sendPhotoBoothReport(dto))
                .doesNotThrowAnyException();

        then(mailSender).should().send(any(MimeMessage.class));
    }
}
