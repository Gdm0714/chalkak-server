package com.min.chalkakserver.exception;

/**
 * 네컷사진관을 찾을 수 없을 때 발생하는 예외
 */
public class PhotoBoothNotFoundException extends RuntimeException {
    
    private final Long photoBoothId;
    
    public PhotoBoothNotFoundException(Long photoBoothId) {
        super(String.format("네컷사진관을 찾을 수 없습니다. ID: %d", photoBoothId));
        this.photoBoothId = photoBoothId;
    }
    
    public PhotoBoothNotFoundException(String message) {
        super(message);
        this.photoBoothId = null;
    }
    
    public Long getPhotoBoothId() {
        return photoBoothId;
    }
}
