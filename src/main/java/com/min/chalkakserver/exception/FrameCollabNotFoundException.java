package com.min.chalkakserver.exception;

/**
 * 프레임 콜라보를 찾을 수 없을 때 발생하는 예외
 */
public class FrameCollabNotFoundException extends RuntimeException {

    private final Long frameCollabId;

    public FrameCollabNotFoundException(Long frameCollabId) {
        super(String.format("프레임 콜라보를 찾을 수 없습니다. ID: %d", frameCollabId));
        this.frameCollabId = frameCollabId;
    }

    public Long getFrameCollabId() {
        return frameCollabId;
    }
}
