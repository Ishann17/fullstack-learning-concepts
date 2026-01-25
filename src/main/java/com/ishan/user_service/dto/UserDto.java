package com.ishan.user_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UserDto {

    @Size(min = 3, message = "First Name must be at least 3 characters long")
    @NotBlank(message = "First Name is required")
    String firstName;

    String lastName;
    String city;

    @Min(value = 1, message = "Age must be greater or equal to 1")
    Integer age;

    @Email(message = "Email format is invalid")
    @NotBlank(message = "Email is required")
    String email;

    String state;
    String phNum;

    @Pattern(regexp = "^(Male|Female)$")
    String gender;
}
/*
@NotBlank = must have real text
@Size = minimum length
@Email = correct email format
@Min = minimum number
@Pattern = allowed values only
@Valid = “activate validation on this request”
*/
