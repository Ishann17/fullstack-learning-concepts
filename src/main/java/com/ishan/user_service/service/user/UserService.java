package com.ishan.user_service.service.user;

import com.ishan.user_service.dto.UserDto;
import com.ishan.user_service.model.User;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.OutputStream;
import java.util.List;

public interface UserService {

    User createNewUser(UserDto userDto);
    User getUserById(int id);
    Page<User> searchUserByAge(int minAge, int maxAge, Pageable pageable);
    Page<User> searchUsers(String name, Integer age, String city, String state, Pageable pageable);
    User updateUser(int id, UserDto userDto);
    User updateUserSpecificField(int id, UserDto userDto);
    void deleteUser(int id);

    User reactivateUser(int id);
}
