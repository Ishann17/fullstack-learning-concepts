package com.ishan.user_service.service;

import com.ishan.user_service.model.User;

import java.util.List;
import java.util.Optional;

public interface UserService {

    User createNewUser(User user);
    User getUserById(int id);
    List<User> searchUserByAge(int minAge, int maxAge);

}
