package com.min.chalkakserver.exception;

import lombok.Getter;

@Getter
public class CollabCandidateNotFoundException extends RuntimeException {

    private final Long candidateId;

    public CollabCandidateNotFoundException(Long candidateId) {
        super(String.format("콜라보 후보를 찾을 수 없습니다. ID: %d", candidateId));
        this.candidateId = candidateId;
    }
}
