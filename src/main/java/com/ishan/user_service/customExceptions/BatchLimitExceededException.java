package com.ishan.user_service.customExceptions;

public class BatchLimitExceededException extends RuntimeException{

    public BatchLimitExceededException(String message){
        super(message);
    }
}
