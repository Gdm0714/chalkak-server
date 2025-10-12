package com.min.chalkakserver.exception;

/**
 * 잘못된 위치 정보로 인한 예외
 */
public class InvalidLocationException extends RuntimeException {
    
    private final Double latitude;
    private final Double longitude;
    
    public InvalidLocationException(String message) {
        super(message);
        this.latitude = null;
        this.longitude = null;
    }
    
    public InvalidLocationException(Double latitude, Double longitude) {
        super(String.format("유효하지 않은 위치입니다. 위도: %f, 경도: %f", latitude, longitude));
        this.latitude = latitude;
        this.longitude = longitude;
    }
    
    public Double getLatitude() {
        return latitude;
    }
    
    public Double getLongitude() {
        return longitude;
    }
}
