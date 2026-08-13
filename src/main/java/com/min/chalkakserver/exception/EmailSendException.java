package com.min.chalkakserver.exception;

/**
 * 메일 전송 실패. 발송 결과를 호출자가 알아야 하는 동기 발송 경로에서 사용한다.
 *
 * <p>비밀번호 재설정 인증코드처럼 사용자가 메일을 기다리는 경우, 실패를 삼키면
 * "발송되었습니다"만 보고 오지 않는 메일을 기다리게 된다.
 */
public class EmailSendException extends RuntimeException {

    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
