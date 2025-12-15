package com.min.chalkakserver.exception;

import lombok.Getter;

@Getter
public class DuplicateReviewException extends RuntimeException {
    
    private final Long photoBoothId;
    
    public DuplicateReviewException(Long photoBoothId) {
        super("이미 이 포토부스에 리뷰를 작성했습니다. 포토부스 ID: " + photoBoothId);
        this.photoBoothId = photoBoothId;
    }
}
