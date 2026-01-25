package com.ishan.user_service.controller;


import com.ishan.user_service.dto.UserDto;
import com.ishan.user_service.model.User;
import com.ishan.user_service.service.user.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;


    @PostMapping
    //@Valid meaning -> Spring, before calling my method, validate this DTO using the rules written on its fields.
    public ResponseEntity<?> createNewUser( @Valid @RequestBody UserDto userDto){
        long startTime = System.currentTimeMillis();
        log.info("[CREATE_USER] Request received | firstName={} age={} gender={} email={}",
                userDto.getFirstName(), userDto.getAge(), userDto.getGender(), userDto.getEmail());

        User newUser = userService.createNewUser(userDto);

        double timeTakenSec = (System.currentTimeMillis() - startTime) / 1000.0;
        log.info("[CREATE_USER] Success | userId={} | timeTakenSec={}",
                newUser.getId(), String.format("%.3f", timeTakenSec));

        URI location = ServletUriComponentsBuilder
                        .fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(newUser.getId())
                        .toUri();

       return ResponseEntity.created(location).body("User successfully created with id :: " + newUser.getId());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable int id){
        User userById = userService.getUserById(id);
        return ResponseEntity.ok(userById);
    }

    @GetMapping
    public ResponseEntity<?> findUsersByAge(@RequestParam int minAge,@RequestParam int maxAge,
                                            @RequestParam (defaultValue = "0") int page,
                                            @RequestParam (defaultValue = "10") int size,
                                            @RequestParam (defaultValue = "name") String sortBy,
                                            @RequestParam (defaultValue = "asc") String direction
    ){

        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page,size,sort);
        Page<User> users = userService.searchUserByAge(minAge, maxAge, pageable);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(@RequestParam (required = false) String name,
                                         @RequestParam (required = false) Integer age,
                                         @RequestParam (required = false) String city,
                                         @RequestParam (required = false) String state,
                                         @RequestParam (defaultValue = "0") int page,
                                         @RequestParam (defaultValue = "10") int size){
        Pageable pageable = PageRequest.of(page, size);
        Page<User> searchedUsers = userService.searchUsers(name, age, city, state, pageable);

        return ResponseEntity.ok(searchedUsers);
    }

    @PutMapping("/{id}")
    public  ResponseEntity<?> updateUser(@PathVariable int id, @Valid @RequestBody UserDto userDto){
        User user = userService.updateUser(id, userDto);
        return ResponseEntity.status(HttpStatus.OK).body(user);
    }


    @PatchMapping("/{id}")
    public  ResponseEntity<?> updateUserSpecificField(@PathVariable int id, @RequestBody UserDto userDto){
        User user = userService.updateUserSpecificField(id, userDto);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{id}")
    public  ResponseEntity<?> deleteUser(@PathVariable int id){
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
