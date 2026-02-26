package com.min.chalkakserver.entity;

public enum ReportStatus {
    PENDING,     // 검토 대기
    REVIEWING,   // 검토 중
    APPROVED,    // 승인 (등록 완료)
    REJECTED,    // 거절
    DUPLICATE    // 중복 제보
}
