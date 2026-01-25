package com.ishan.user_service.service.user;


import com.ishan.user_service.customExceptions.UserNotFoundException;
import com.ishan.user_service.dto.UserDto;
import com.ishan.user_service.mapper.UserDtoToUserMapper;
import com.ishan.user_service.model.User;
import com.ishan.user_service.repository.UserRepository;
import com.ishan.user_service.specification.UserSpecification;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Override
    public User createNewUser(UserDto userDto) {
        User user = UserDtoToUserMapper.convertUserDtoToUser(userDto);
        return userRepository.save(user);
    }

    @Override
    public User getUserById(int id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
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

    @Override
    public User updateUser(int id, UserDto userDto) {
        User existingUser = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        User updatedUser = UserDtoToUserMapper.updateExistingUserWithUserDto(userDto, existingUser);
        return userRepository.save(updatedUser);

    }

    @Override
    public User updateUserSpecificField(int id, UserDto userDto) {

        User existing = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));

        if (userDto.getFirstName() != null && !userDto.getFirstName().isBlank())
            existing.setName(userDto.getFirstName());

        if (userDto.getEmail() != null && !userDto.getEmail().isBlank())
            existing.setEmail(userDto.getEmail());

        if (userDto.getCity() != null)
            existing.setCity(userDto.getCity());

        if (userDto.getState() != null)
            existing.setState(userDto.getState());

        if (userDto.getAge() >= 1)
            existing.setAge(userDto.getAge());

        if (userDto.getPhNum() != null)
            existing.setMobileNumber(userDto.getPhNum());

        if (userDto.getGender() != null)
            existing.setGender(userDto.getGender());

        return userRepository.save(existing);
    }

    @Override
    public void deleteUser(int id) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        userRepository.delete(user);
    }

}


