package com.ishan.user_service.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ishan.user_service.dto.UserDto;
import com.ishan.user_service.mapper.UserMapperFromRandomToDto;
import com.ishan.user_service.model.User;
import com.ishan.user_service.service.RandomUserClientService;
import com.ishan.user_service.service.UserImportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * This controller is responsible for importing a user
 * from an external source (Random User API) into our system.
 *
 * NOTE:
 * - Orchestration logic is intentionally kept in the controller
 *   for learning purposes.
 * - In production, this would typically move to a service layer.
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserImportController {

    // Service that handles persistence and business rules
    private final UserImportService userImportService;

    // Client service responsible for calling the external API
    private final RandomUserClientService randomUserClientService;

    /**
     * ObjectMapper is used to convert raw JSON text into
     * a navigable JSON tree (JsonNode).
     *
     * We are using manual parsing instead of direct binding
     * to understand how JSON traversal actually works.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Constructor injection ensures:
     * - Dependencies are mandatory
     * - The class is easier to test
     * - Avoids field injection pitfalls
     */
    public UserImportController(UserImportService userImportService,
                                RandomUserClientService randomUserClientService) {
        this.userImportService = userImportService;
        this.randomUserClientService = randomUserClientService;
    }

    /**
     * POST endpoint to import a single user from an external API.
     * Why POST?
     * - This operation creates a new resource in our database
     * - It has side effects
     * - Each call results in a new user being created
     */
    @PostMapping("/import")
    public ResponseEntity<?> importUserFromExternalSource() throws JsonProcessingException {

        // STEP 1: Call the third-party API and get raw JSON response
        String fetchRandomUserRaw = randomUserClientService.fetchRandomUsersRaw();

        // STEP 2: Parse the JSON string into a navigable JSON tree
        JsonNode navigableTree = objectMapper.readTree(fetchRandomUserRaw);

        // STEP 3: Navigate to the first user inside the "results" array
        JsonNode userRawData = navigableTree.get("results").get(0);

        // STEP 4: Convert raw JSON data into a UserDto
        UserDto userDto = UserMapperFromRandomToDto.convertRandomUserToUserDto(userRawData);

        // STEP 5: Persist the user and get the saved entity
        User user = userImportService.importSingleUserFromExternalSource(userDto);

        /**
         * STEP 6: Build the Location URI for the newly created resource
         * ServletUriComponentsBuilder.fromCurrentRequest()
         * - Takes the current request URL:
         *   /api/v1/users/import
         * .path("/{id}")
         * - Appends a path variable placeholder
         * .buildAndExpand(user.getId())
         * - Replaces {id} with the actual generated user ID
         * Resulting URI example:
         * http://localhost:8080/api/v1/users/import/15
         * This URI is returned in the Location header
         * as per REST standards for resource creation.
         */
        URI uriLocation = ServletUriComponentsBuilder
                        .fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(user.getId())
                        .toUri();

        /**
         * Return:
         * - HTTP 201 Created
         * - Location header pointing to the newly created resource
         * - Response body (for learning/visibility)
         */
        return ResponseEntity.created(uriLocation).body("User Created Successfully! " + user);
    }


    /**
     * Imports multiple users from an external source (Random User API)
     * and persists them in the database in a single batch transaction.
     * We use @RequestParam instead of @PathVariable here because:
     * - 'count' does NOT identify a resource
     * - It only modifies how the operation behaves
     * - The resource (/import/batch) remains the same
     * PathVariable is used when the variable is part of the
     * resource identity, for example:
     *   /users/{id}
     * QueryParam is used for:
     * - filters
     * - limits
     * - pagination
     * - batch size (this case)
     * defaultValue = "10" ensures:
     * - API works even if client does not pass 'count'
     * - Prevents accidental large imports
     */
    @PostMapping("/import/batch")
    public ResponseEntity<?> importMultipleUsersFromExternalSource(@RequestParam(defaultValue = "10") int count) throws JsonProcessingException {

        // STEP 1: Call external API to fetch 'count' users in ONE request
        // Example URL generated internally:
        // https://randomuser.me/api/?results=10&nat=us,ca,au,gb,in
        String multipleRandomUsersRaw =
                randomUserClientService.fetchMultipleRandomUsersRaw(count);

        // STEP 2: Convert raw JSON String into a navigable JSON tree
        JsonNode navigableTree =
                objectMapper.readTree(multipleRandomUsersRaw);

        // STEP 3: Extract the "results" array from the response
        // This array contains multiple user objects
        JsonNode rawUsersDataArray =
                navigableTree.get("results");

        // STEP 4: Convert JSON array → List<UserDto> using mapper
        // Mapper is responsible ONLY for data transformation
        List<UserDto> userDtoList =
                UserMapperFromRandomToDto.convertRandomUsersToUserDtoList(rawUsersDataArray);

        // STEP 5: Persist all users in a single transaction using saveAll()
        // If any insert fails, the entire batch is rolled back
        userImportService.importMultipleUsersFromExternalSource(userDtoList);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("Multiple Users " + userDtoList.size() + " total Users added");
    }


}
