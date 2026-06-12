package com.min.chalkakserver.exception;

import lombok.Getter;

@Getter
public class DuplicateAvailabilityReportException extends RuntimeException {
    private final Long frameCollabId;
    private final Long photoBoothId;

    public DuplicateAvailabilityReportException(Long frameCollabId, Long photoBoothId) {
        super("이미 최근 1시간 내에 이 매장의 프레임 재고를 제보했습니다.");
        this.frameCollabId = frameCollabId;
        this.photoBoothId = photoBoothId;
    }
}
