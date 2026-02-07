package com.ishan.user_service.customExceptions;

import lombok.Getter;

@Getter
public class CooldownActiveException extends RuntimeException{

    private final long remainingSeconds;
    private final long totalSeconds;

    public CooldownActiveException(String message, long totalSeconds, long remainingSeconds) {
        super(message);
        this.totalSeconds = totalSeconds;
        this.remainingSeconds = remainingSeconds;

    }

}
