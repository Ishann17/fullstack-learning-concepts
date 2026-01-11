package com.ishan.user_service.customExceptions;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(int id) {
        super("User not found with id: " + id);
    }
}
