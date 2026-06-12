package com.min.chalkakserver.util;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 콜라보 공지 텍스트에서 제목/브랜드/아티스트/기간을 휴리스틱으로 추출하는 파서.
 * 완벽한 추출이 목적이 아니라 어드민 검토용 초안 생성이 목적이다.
 */
public final class CollabTextParser {

    private static final List<String> KNOWN_BRANDS = List.of(
            "인생네컷", "포토이즘", "하루필름", "포토그레이", "포토매틱",
            "모노맨션", "포토시그니처", "셀픽스", "비룸", "돈룩업", "포토드링크",
            "플랜비스튜디오", "포토하임", "금달스튜디오", "홍대네컷", "포토오브제"
    );

    // 연도 포함 날짜: 2026.06.01 / 2026-06-01 / 2026년 6월 1일
    private static final Pattern FULL_DATE = Pattern.compile(
            "(\\d{4})\\s*[.\\-/년]\\s*(\\d{1,2})\\s*[.\\-/월]\\s*(\\d{1,2})\\s*일?");

    // 연도 생략 날짜: 6월 1일 / 06.01 / 6/1
    private static final Pattern SHORT_DATE = Pattern.compile(
            "(?<!\\d)(\\d{1,2})\\s*[./월]\\s*(\\d{1,2})\\s*일?(?![./\\d])");

    private static final Pattern COLLAB_SEPARATOR = Pattern.compile("\\s*[xX×]\\s*");

    private CollabTextParser() {
    }

    public record ParsedCollab(String title, String brand, String artistName,
                               LocalDate startDate, LocalDate endDate) {
    }

    public static ParsedCollab parse(String rawText, LocalDate referenceDate) {
        String text = rawText == null ? "" : rawText.strip();

        String title = extractTitle(text);
        String brand = extractBrand(text);
        String artistName = extractArtistName(title, brand);
        List<LocalDate> dates = extractDates(text, referenceDate);

        LocalDate startDate = dates.size() >= 1 ? dates.get(0) : null;
        LocalDate endDate = dates.size() >= 2 ? dates.get(1) : null;
        // 12월 시작 ~ 1월 종료처럼 연도 생략 표기에서 종료일이 시작일보다 앞서면 이듬해로 보정
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            endDate = endDate.plusYears(1);
        }

        return new ParsedCollab(title, brand, artistName, startDate, endDate);
    }

    private static String extractTitle(String text) {
        for (String line : text.split("\\R")) {
            String trimmed = line.strip();
            if (!trimmed.isEmpty()) {
                return trimmed.length() > 150 ? trimmed.substring(0, 150) : trimmed;
            }
        }
        return null;
    }

    private static String extractBrand(String text) {
        for (String brand : KNOWN_BRANDS) {
            if (text.contains(brand)) {
                return brand;
            }
        }
        return null;
    }

    private static String extractArtistName(String title, String brand) {
        if (title == null || brand == null || !title.contains(brand)) {
            return null;
        }
        String[] parts = COLLAB_SEPARATOR.split(title);
        if (parts.length < 2) {
            return null;
        }
        for (String part : parts) {
            String candidate = part.strip();
            if (!candidate.isEmpty() && !candidate.contains(brand)) {
                return candidate.length() > 100 ? candidate.substring(0, 100) : candidate;
            }
        }
        return null;
    }

    private static List<LocalDate> extractDates(String text, LocalDate referenceDate) {
        List<LocalDate> dates = new ArrayList<>();
        StringBuilder remaining = new StringBuilder(text);

        Matcher full = FULL_DATE.matcher(text);
        while (full.find() && dates.size() < 2) {
            LocalDate parsed = safeDate(
                    Integer.parseInt(full.group(1)),
                    Integer.parseInt(full.group(2)),
                    Integer.parseInt(full.group(3)));
            if (parsed != null) {
                dates.add(parsed);
                blankOut(remaining, full.start(), full.end());
            }
        }

        if (dates.size() < 2) {
            Matcher shortDate = SHORT_DATE.matcher(remaining.toString());
            while (shortDate.find() && dates.size() < 2) {
                LocalDate parsed = safeDate(
                        referenceDate.getYear(),
                        Integer.parseInt(shortDate.group(1)),
                        Integer.parseInt(shortDate.group(2)));
                if (parsed != null) {
                    dates.add(parsed);
                }
            }
        }
        return dates;
    }

    private static void blankOut(StringBuilder text, int start, int end) {
        for (int i = start; i < end; i++) {
            text.setCharAt(i, ' ');
        }
    }

    private static LocalDate safeDate(int year, int month, int day) {
        try {
            return LocalDate.of(year, month, day);
        } catch (Exception e) {
            return null;
        }
    }
}
