package com.ishan.user_service.customExceptions;

public class TooManyRequestsException extends RuntimeException{

    public TooManyRequestsException(String message) {
        super(message);
    }
}
