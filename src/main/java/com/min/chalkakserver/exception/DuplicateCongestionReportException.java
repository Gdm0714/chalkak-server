package com.min.chalkakserver.exception;

import lombok.Getter;

@Getter
public class DuplicateCongestionReportException extends RuntimeException {
    private final Long photoBoothId;

    public DuplicateCongestionReportException(Long photoBoothId) {
        super("이미 최근 1시간 내에 이 포토부스의 혼잡도를 제보했습니다.");
        this.photoBoothId = photoBoothId;
    }
}
