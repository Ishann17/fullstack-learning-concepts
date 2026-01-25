package com.ishan.user_service.mapper;

import com.ishan.user_service.dto.UserDto;
import com.ishan.user_service.model.User;

import java.util.Objects;

public class UserDtoToUserMapper {

    public static User convertUserDtoToUser(UserDto userDto){
        User user = new User();
        user.setAge(userDto.getAge());
        user.setCity(userDto.getCity());
        user.setGender(userDto.getGender());
        if(!Objects.isNull(userDto.getPhNum())){
            String normalizedPhone = userDto.getPhNum().replaceAll("\\D", "");
            user.setMobileNumber(normalizedPhone);
        }
        user.setState(userDto.getState());
        user.setEmail(userDto.getEmail());
        user.setFirstName(userDto.getFirstName());
        user.setLastName(userDto.getLastName());

        return user;
    }

    public static User updateExistingUserWithUserDto(UserDto userDto, User existingUser){

        String last = (userDto.getLastName() == null) ? "" : userDto.getLastName();
        existingUser.setFirstName(userDto.getFirstName());
        existingUser.setLastName(userDto.getLastName());
        existingUser.setEmail(userDto.getEmail());
        existingUser.setCity(userDto.getCity());
        existingUser.setState(userDto.getState());
        existingUser.setAge(userDto.getAge());
        existingUser.setMobileNumber(userDto.getPhNum());
        existingUser.setGender(userDto.getGender());

        return existingUser;
    }
}
