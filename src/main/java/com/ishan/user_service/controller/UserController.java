package com.ishan.user_service.controller;


import com.ishan.user_service.model.User;
import com.ishan.user_service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

}
