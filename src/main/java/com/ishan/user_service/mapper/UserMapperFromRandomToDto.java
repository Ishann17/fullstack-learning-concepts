package com.ishan.user_service.mapper;


import com.fasterxml.jackson.databind.JsonNode;
import com.ishan.user_service.dto.UserDto;

import java.util.ArrayList;
import java.util.List;

public class UserMapperFromRandomToDto {

    public static UserDto convertRandomUserToUserDto(JsonNode randomUser){
        UserDto userDto = new UserDto();
        String fName = randomUser.get("name").get("first").asText();
        String lName = randomUser.get("name").get("last").asText();
        String email = randomUser.get("email").asText();
        String city = randomUser.get("location").get("city").asText();
        String state = randomUser.get("location").get("state").asText();
        int age = randomUser.get("dob").get("age").asInt();
        String phNum = randomUser.get("cell").asText();
        String gender = randomUser.get("gender").asText();

        /*
        What this means: \\D → “anything that is NOT a digit” Replace with empty string
        Phone numbers from third-party APIs often come in different formats. I handle this at the service layer by normalizing the value—for example, removing non-digit characters—before validation or persistence
        */
        String normalizedPhone = phNum.replaceAll("\\D", "");

        userDto.setFirstName(fName);
        userDto.setLastName(lName);
        userDto.setEmail(email);
        userDto.setCity(city);
        userDto.setAge(age);
        userDto.setState(state);
        userDto.setPhNum(normalizedPhone);
        userDto.setGender(gender);

       // System.out.println("USER DATA FROM API" + userDto);

        return userDto;
    }

    /**
     * Converts the "results" JSON array into a list of UserDto.
     *
     * @param resultsArray JsonNode representing results[]
     * @return list of mapped UserDto objects
     */
    public static List<UserDto> convertRandomUsersToUserDtoList(JsonNode resultsArray){
        List<UserDto> userDtosList = new ArrayList<>();

        for(JsonNode userNode : resultsArray){
            UserDto userDto = convertRandomUserToUserDto(userNode);
            userDtosList.add(userDto);
        }
        return userDtosList;
    }

}
