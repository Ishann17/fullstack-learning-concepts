package com.ishan.user_service.service;

import com.ishan.user_service.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UserService {

    User createNewUser(User user);
    User getUserById(int id);
    List<User> exportAllUsersToCSV();
    Page<User> searchUserByAge(int minAge, int maxAge, Pageable pageable);
    Page<User> searchUsers(String name, Integer age, String city, String state, Pageable pageable);
}
