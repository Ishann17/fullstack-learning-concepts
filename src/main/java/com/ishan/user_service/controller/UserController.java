package com.ishan.user_service.controller;


import com.ishan.user_service.model.User;
import com.ishan.user_service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Autowired
    private UserService userService;


    /*@PostMapping
    public ResponseEntity<?> createNewUser(@RequestBody User user){
        User newUser = userService.createNewUser(user);

        URI location = ServletUriComponentsBuilder
                        .fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(newUser.getId())
                        .toUri();

       return ResponseEntity.created(location).body("User Got Created !");
    }*/

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable int id){
        User userById = userService.getUserById(id);
        return ResponseEntity.ok(userById);
    }

    @GetMapping
    public ResponseEntity<?> findUsersByAge(@RequestParam int minAge,@RequestParam int maxAge){
        List<User> users = userService.searchUserByAge(minAge, maxAge);
        return null;
    }

}
