package com.min.chalkakserver.exception;

import lombok.Getter;

@Getter
public class PhotobookEntryNotFoundException extends RuntimeException {

    private final Long entryId;

    public PhotobookEntryNotFoundException(Long entryId) {
        super(String.format("포토북 사진을 찾을 수 없습니다. ID: %d", entryId));
        this.entryId = entryId;
    }
}
