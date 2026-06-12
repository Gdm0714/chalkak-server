package com.min.chalkakserver.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CollabTextParser 테스트")
class CollabTextParserTest {

    private static final LocalDate REFERENCE = LocalDate.of(2026, 6, 12);

    @Test
    @DisplayName("연도 포함 기간과 브랜드, 아티스트를 추출한다")
    void parse_FullDates() {
        String text = """
                뉴진스 X 인생네컷 한정판 프레임 출시!
                기간: 2026.06.01 ~ 2026.06.30
                전국 매장에서 만나보세요.
                """;

        CollabTextParser.ParsedCollab result = CollabTextParser.parse(text, REFERENCE);

        assertThat(result.title()).isEqualTo("뉴진스 X 인생네컷 한정판 프레임 출시!");
        assertThat(result.brand()).isEqualTo("인생네컷");
        assertThat(result.artistName()).isEqualTo("뉴진스");
        assertThat(result.startDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(result.endDate()).isEqualTo(LocalDate.of(2026, 6, 30));
    }

    @Test
    @DisplayName("한국어 날짜 표기를 파싱한다")
    void parse_KoreanDates() {
        String text = "에스파 × 포토이즘 콜라보\n2026년 7월 1일부터 2026년 7월 15일까지";

        CollabTextParser.ParsedCollab result = CollabTextParser.parse(text, REFERENCE);

        assertThat(result.brand()).isEqualTo("포토이즘");
        assertThat(result.artistName()).isEqualTo("에스파");
        assertThat(result.startDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(result.endDate()).isEqualTo(LocalDate.of(2026, 7, 15));
    }

    @Test
    @DisplayName("연도 생략 날짜는 기준 연도로 채운다")
    void parse_ShortDates() {
        String text = "하루필름 신규 프레임\n6월 20일 ~ 7월 10일";

        CollabTextParser.ParsedCollab result = CollabTextParser.parse(text, REFERENCE);

        assertThat(result.startDate()).isEqualTo(LocalDate.of(2026, 6, 20));
        assertThat(result.endDate()).isEqualTo(LocalDate.of(2026, 7, 10));
    }

    @Test
    @DisplayName("연말에 걸친 기간은 종료일을 이듬해로 보정한다")
    void parse_YearRollover() {
        String text = "포토그레이 윈터 에디션\n12월 20일 ~ 1월 5일";

        CollabTextParser.ParsedCollab result = CollabTextParser.parse(text, REFERENCE);

        assertThat(result.startDate()).isEqualTo(LocalDate.of(2026, 12, 20));
        assertThat(result.endDate()).isEqualTo(LocalDate.of(2027, 1, 5));
    }

    @Test
    @DisplayName("브랜드와 날짜가 없으면 제목만 추출한다")
    void parse_TitleOnly() {
        String text = "신규 콜라보 공지 예정";

        CollabTextParser.ParsedCollab result = CollabTextParser.parse(text, REFERENCE);

        assertThat(result.title()).isEqualTo("신규 콜라보 공지 예정");
        assertThat(result.brand()).isNull();
        assertThat(result.artistName()).isNull();
        assertThat(result.startDate()).isNull();
        assertThat(result.endDate()).isNull();
    }

    @Test
    @DisplayName("빈 텍스트는 모든 필드가 null이다")
    void parse_Empty() {
        CollabTextParser.ParsedCollab result = CollabTextParser.parse("  \n ", REFERENCE);

        assertThat(result.title()).isNull();
        assertThat(result.brand()).isNull();
    }
}
