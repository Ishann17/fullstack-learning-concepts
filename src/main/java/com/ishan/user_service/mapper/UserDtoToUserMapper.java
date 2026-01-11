package com.ishan.user_service.mapper;

import com.ishan.user_service.dto.UserDto;
import com.ishan.user_service.model.User;

public class UserDtoToUserMapper {

    public static User convertUserDtoToUser(UserDto userDto){
        User user = new User();
        user.setAge(userDto.getAge());
        user.setCity(userDto.getCity());
        user.setGender(userDto.getGender());
        user.setMobileNumber(userDto.getPhNum());
        user.setState(userDto.getState());
        user.setEmail(userDto.getEmail());
        user.setName(userDto.getFirstName() + " " + userDto.getLastName());

        return user;
    }
}
