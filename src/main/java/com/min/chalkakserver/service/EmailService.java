package com.min.chalkakserver.service;

import com.min.chalkakserver.dto.PhotoBoothReportDto;
import com.min.chalkakserver.exception.EmailSendException;
import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.admin-email}")
    private String adminEmail;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    /** .env.example의 예시 값. 이 값이 그대로 들어와 있으면 설정이 안 된 것이다. */
    private static final Set<String> PLACEHOLDERS =
        Set.of("your_email@gmail.com", "your_app_password_here", "changeme");

    /**
     * 메일 자격증명 누락을 시작 시점에 드러낸다.
     *
     * <p>앱을 죽이지는 않는다. 메일은 일부 기능(제보·비밀번호 재설정)만 쓰므로,
     * 이것 때문에 API 전체를 내리는 편이 더 나쁘다. 대신 실패를 조용히 넘기지 않도록
     * ERROR로 남긴다.
     */
    @PostConstruct
    void warnIfMailNotConfigured() {
        boolean missing = isUnset(mailUsername) || isUnset(mailPassword);
        if (missing) {
            log.error(
                "메일 자격증명이 설정되지 않았습니다 (MAIL_USERNAME/MAIL_PASSWORD). "
                    + "제보 메일과 비밀번호 재설정 인증코드가 발송되지 않습니다. "
                    + "MAIL_PASSWORD는 Gmail 계정 비밀번호가 아니라 앱 비밀번호여야 합니다.");
        }
    }

    private static boolean isUnset(String value) {
        return value == null || value.isBlank() || PLACEHOLDERS.contains(value.trim());
    }

    @Async
    public void sendPhotoBoothReport(PhotoBoothReportDto reportDto) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(adminEmail);
            helper.setSubject("[찰칵] 새로운 네컷사진관 제보");
            helper.setText(buildReportEmailContent(reportDto), true);

            mailSender.send(message);
            log.info("제보 이메일 전송 성공: {}", escapeForLog(reportDto.getName()));
        } catch (MessagingException | RuntimeException e) {
            // @Async 메서드에서는 예외를 던져도 호출자에게 전달되지 않으므로 로깅만 수행
            log.error("이메일 전송 실패: {}", escapeForLog(reportDto.getName()), e);
        }
    }

    /**
     * 비밀번호 재설정 인증코드 발송. 사용자가 결과를 기다리므로 동기로 보내고
     * 실패는 {@link EmailSendException}으로 알린다 (@Async로 두면 실패가 묻힌다).
     */
    public void sendPasswordResetCode(String toEmail, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("[찰칵] 비밀번호 재설정 인증코드");
            helper.setText(buildPasswordResetEmailContent(code), true);

            mailSender.send(message);
            log.info("비밀번호 재설정 인증코드 전송 성공: {}", escapeForLog(toEmail));
        } catch (MessagingException | RuntimeException e) {
            log.error("비밀번호 재설정 인증코드 전송 실패: {}", escapeForLog(toEmail), e);
            throw new EmailSendException("비밀번호 재설정 인증코드 전송 실패", e);
        }
    }

    private String buildPasswordResetEmailContent(String code) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<h2>비밀번호 재설정 인증코드</h2>");
        sb.append("<p>아래 6자리 인증코드를 입력해 비밀번호를 재설정해주세요.</p>");
        sb.append("<p style='font-size: 28px; font-weight: bold; letter-spacing: 4px;'>")
            .append(escapeHtml(code))
            .append("</p>");
        sb.append("<p style='color: gray;'>이 인증코드는 5분간 유효합니다.</p>");
        sb.append("<hr>");
        sb.append(
            "<p style='color: gray; font-size: 12px;'>본인이 요청하지 않았다면 이 메일을 무시하셔도 됩니다.</p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    /**
     * HTML 이스케이프 처리 - XSS 방지
     */
    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return HtmlUtils.htmlEscape(input);
    }

    /**
     * URL 인코딩 처리
     */
    private String encodeUrl(String input) {
        if (input == null) {
            return "";
        }
        return URLEncoder.encode(input, StandardCharsets.UTF_8);
    }

    /**
     * 로그용 문자열 이스케이프 (로그 인젝션 방지)
     */
    private String escapeForLog(String input) {
        if (input == null) {
            return "";
        }
        return input.replaceAll("[\n\r\t]", "_");
    }

    private String buildReportEmailContent(PhotoBoothReportDto reportDto) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<h2>새로운 네컷사진관 제보</h2>");
        sb.append("<hr>");

        sb.append("<h3>기본 정보</h3>");
        sb.append("<table border='1' cellpadding='10' style='border-collapse: collapse;'>");
        sb.append("<tr><th>항목</th><th>내용</th></tr>");
        sb.append("<tr><td>이름</td><td>").append(escapeHtml(reportDto.getName())).append("</td></tr>");

        if (reportDto.getBrand() != null) {
            sb.append("<tr><td>브랜드</td><td>").append(escapeHtml(reportDto.getBrand())).append("</td></tr>");
        }

        if (reportDto.getSeries() != null) {
            sb.append("<tr><td>시리즈</td><td>").append(escapeHtml(reportDto.getSeries())).append("</td></tr>");
        }

        sb.append("<tr><td>주소</td><td>").append(escapeHtml(reportDto.getAddress())).append("</td></tr>");

        if (reportDto.getRoadAddress() != null) {
            sb.append("<tr><td>도로명 주소</td><td>").append(escapeHtml(reportDto.getRoadAddress()))
                .append("</td></tr>");
        }

        sb.append("<tr><td>위도</td><td>").append(reportDto.getLatitude()).append("</td></tr>");
        sb.append("<tr><td>경도</td><td>").append(reportDto.getLongitude()).append("</td></tr>");

        if (reportDto.getPriceInfo() != null) {
            sb.append("<tr><td>가격 정보</td><td>").append(escapeHtml(reportDto.getPriceInfo()))
                .append("</td></tr>");
        }

        if (reportDto.getDescription() != null) {
            sb.append("<tr><td>설명</td><td>").append(escapeHtml(reportDto.getDescription()))
                .append("</td></tr>");
        }

        sb.append("</table>");

        if (reportDto.getReporterName() != null || reportDto.getReporterEmail() != null) {
            sb.append("<h3>제보자 정보</h3>");
            sb.append("<table border='1' cellpadding='10' style='border-collapse: collapse;'>");
            sb.append("<tr><th>항목</th><th>내용</th></tr>");

            if (reportDto.getReporterName() != null) {
                sb.append("<tr><td>이름</td><td>").append(escapeHtml(reportDto.getReporterName()))
                    .append("</td></tr>");
            }

            if (reportDto.getReporterEmail() != null) {
                sb.append("<tr><td>이메일</td><td>").append(escapeHtml(reportDto.getReporterEmail()))
                    .append("</td></tr>");
            }

            sb.append("</table>");
        }

        // 지도 링크 (URL 인코딩 적용)
        sb.append("<h3>지도 확인</h3>");
        sb.append("<p><a href='https://map.naver.com/v5/search/")
            .append(encodeUrl(reportDto.getAddress()))
            .append("'>네이버 지도에서 보기</a></p>");
        sb.append("<p><a href='https://www.google.com/maps/search/?api=1&query=")
            .append(reportDto.getLatitude()).append(",").append(reportDto.getLongitude())
            .append("'>구글 지도에서 보기</a></p>");

        sb.append("<hr>");
        sb.append(
            "<p style='color: gray; font-size: 12px;'>이 메일은 찰칵 앱의 네컷사진관 제보 기능을 통해 자동 전송되었습니다.</p>");
        sb.append("</body></html>");

        return sb.toString();
    }
}
