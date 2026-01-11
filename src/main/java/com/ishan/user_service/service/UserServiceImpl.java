package com.ishan.user_service.service;


import com.ishan.user_service.customExceptions.UserNotFoundException;
import com.ishan.user_service.model.User;
import com.ishan.user_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public User createNewUser(User user) {
        User save = userRepository.save(user);
        return save;
    }

    @Override
    public User getUserById(int id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    @Override
    public Page<User> searchUserByAge(int minAge, int maxAge, Pageable pageable) {
        return userRepository.findByAgeBetween(minAge, maxAge, pageable);
    }
}
