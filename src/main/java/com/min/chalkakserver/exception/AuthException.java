package com.min.chalkakserver.exception;

import lombok.Getter;

@Getter
public class AuthException extends RuntimeException {
    
    private final String code;
    
    public AuthException(String message) {
        super(message);
        this.code = "AUTH_ERROR";
    }

    public AuthException(String message, String code) {
        super(message);
        this.code = code;
    }

    public AuthException(String message, Throwable cause) {
        super(message, cause);
        this.code = "AUTH_ERROR";
    }
}
