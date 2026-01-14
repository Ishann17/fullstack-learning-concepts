package com.ishan.user_service.service;


import com.ishan.user_service.customExceptions.UserNotFoundException;
import com.ishan.user_service.model.User;
import com.ishan.user_service.repository.UserRepository;
import com.ishan.user_service.specification.UserSpecification;
import com.ishan.user_service.utility.CSVReadWriteUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

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
    public List<User> exportAllUsersToCSV() {

        List<User> userList = userRepository.findAll();
        CSVReadWriteUtility.writeCSV(userList);

        return null;
    }

    @Override
    public Page<User> searchUserByAge(int minAge, int maxAge, Pageable pageable) {
        return userRepository.findByAgeBetween(minAge, maxAge, pageable);
    }

    @Override
    public Page<User> searchUsers(String name, Integer age, String city, String state, Pageable pageable) {

        // Start with a specification that matches ALL records
        Specification<User> spec = Specification.allOf();

        if(Objects.nonNull(name) && !name.isBlank()){
            spec = spec.and(UserSpecification.hasName(name));
        }
        if(Objects.nonNull(city) && !city.isBlank()){
            spec = spec.and(UserSpecification.hasCity(city));
        }
        if(Objects.nonNull(state) && !state.isBlank()){
            spec = spec.and(UserSpecification.hasState(state));
        }
        if(Objects.nonNull(age)){
            spec = spec.and(UserSpecification.hasAge(age));
        }

        return userRepository.findAll(spec, pageable);
    }
}
