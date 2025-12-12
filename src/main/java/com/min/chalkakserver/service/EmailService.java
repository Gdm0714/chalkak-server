package com.min.chalkakserver.service;

import com.min.chalkakserver.dto.PhotoBoothReportDto;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.admin-email}")
    private String adminEmail;

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
        } catch (MessagingException e) {
            // @Async 메서드에서는 예외를 던져도 호출자에게 전달되지 않으므로 로깅만 수행
            log.error("이메일 전송 실패: {}", escapeForLog(reportDto.getName()), e);
        }
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
