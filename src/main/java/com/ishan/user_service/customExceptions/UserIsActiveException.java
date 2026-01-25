package com.ishan.user_service.customExceptions;

public class UserIsActiveException extends RuntimeException{

    public UserIsActiveException(int id){
        super("User with id :: " + id + " is active!");
    }
}
