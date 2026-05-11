package com.min.chalkakserver.exception;

import lombok.Getter;

@Getter
public class PostNotFoundException extends RuntimeException {

    private final Long postId;

    public PostNotFoundException(Long postId) {
        super("게시물을 찾을 수 없습니다. ID: " + postId);
        this.postId = postId;
    }
}
