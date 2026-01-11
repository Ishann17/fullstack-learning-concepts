package com.ishan.user_service.service;

import com.ishan.user_service.dto.UserDto;
import com.ishan.user_service.mapper.UserDtoToUserMapper;
import com.ishan.user_service.mapper.UserMapperFromRandomToDto;
import com.ishan.user_service.model.User;
import com.ishan.user_service.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.beans.Transient;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserImportServiceImpl implements UserImportService{

    @Autowired
    private UserRepository userRepository;


    @Override
    public User importSingleUserFromExternalSource(UserDto userDto) {
        User user = UserDtoToUserMapper.convertUserDtoToUser(userDto);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void importMultipleUsersFromExternalSource(List<UserDto> userDtoList) {

        List<User> userList = new ArrayList<>();
        for(UserDto userDto: userDtoList){
            userList.add(UserDtoToUserMapper.convertUserDtoToUser(userDto));
        }
        userRepository.saveAll(userList);
    }
}
