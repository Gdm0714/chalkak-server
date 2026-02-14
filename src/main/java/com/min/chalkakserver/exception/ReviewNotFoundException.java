package com.min.chalkakserver.exception;

import lombok.Getter;

@Getter
public class ReviewNotFoundException extends RuntimeException {
    
    private final Long reviewId;
    
    public ReviewNotFoundException(Long reviewId) {
        super("리뷰를 찾을 수 없습니다. ID: " + reviewId);
        this.reviewId = reviewId;
    }
}
