package com.min.chalkakserver.exception;

/**
 * 중복된 네컷사진관 데이터로 인한 예외
 */
public class DuplicatePhotoBoothException extends RuntimeException {
    
    private final String name;
    private final String address;
    
    public DuplicatePhotoBoothException(String name, String address) {
        super(String.format("이미 등록된 네컷사진관입니다. 이름: %s, 주소: %s", name, address));
        this.name = name;
        this.address = address;
    }
    
    public String getName() {
        return name;
    }
    
    public String getAddress() {
        return address;
    }
}
