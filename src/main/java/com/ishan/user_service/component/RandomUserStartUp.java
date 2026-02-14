/*

//THIS CLASS IS FOR TESTING ONLY
package com.ishan.user_service.component;

 import com.fasterxml.jackson.databind.ObjectMapper;
 import com.ishan.user_service.component.rateLimit.ImportJobCostTier;
 import com.ishan.user_service.component.redis.RedisStore;
 import com.ishan.user_service.service.RandomUserClientService;
 import org.springframework.boot.CommandLineRunner;
 import org.springframework.stereotype.Component;

*/
/*
 * @Component marks this class as a Spring-managed bean.
 *
 * Why Component?
 * - So Spring can automatically detect and instantiate this class
 * - So it can participate in the application lifecycle
 * - Required for CommandLineRunner to be executed at startup
 *//*

@Component
public class RandomUserStartUp implements CommandLineRunner {

    */
/**
     * Why CommandLineRunner?
     * - It allows running code once when the Spring Boot application starts
     * - Useful for testing integrations, loading data, or running startup logic
     * - Avoids creating REST endpoints just for testing
     *//*


    // Service responsible for calling the third-party Random User API
    private final RandomUserClientService randomUserClientService;
    private final RedisStore redisStore;

    */
/**
     * ObjectMapper is used to parse raw JSON text (String)
     * into a navigable JSON tree (JsonNode).
     *
     * This allows manual traversal of JSON instead of automatic binding.
     *//*

    private final ObjectMapper objectMapper = new ObjectMapper();

    */
/**
     * Constructor injection:
     * - Preferred way of dependency injection in Spring
     * - Makes the dependency explicit and immutable (final)
     * - Easier to test and avoids field injection
     *//*

    public RandomUserStartUp(RandomUserClientService randomUserClientService, RedisStore redisStore) {
        this.randomUserClientService = randomUserClientService;
        this.redisStore = redisStore;
    }

    */
/**
     * This method is executed automatically once the application starts.
     *//*

    @Override
    public void run(String... args) throws Exception {

        // Step 1: Call the third-party API and get raw JSON response as String
        //String response = randomUserClientService.fetchRandomUsersRaw();

        // Step 2: Convert JSON text into a JSON tree structure
        //JsonNode rootNode = objectMapper.readTree(response);

        // Step 3: Navigate the JSON tree to access the first user in results array
        //JsonNode userNode = rootNode.get("results").get(0);

        // Step 4: Manually map JSON data to a UserDto using mapper class
        //UserDto userDto = UserMapperFromRandomToDto.convertRandomUserToUserDto(userNode);

        // Step 5: Print the mapped DTO to verify end-to-end flow
        //System.out.println("USER DTO :: " + userDto);

        String user1 = "ishan";
        String user2 = "rahul";

        System.out.println( "[--COUNT--] = " + redisStore.countRunningJobs(user1, ImportJobCostTier.SMALL));
        System.out.println( "[--COUNT--] = " + redisStore.countRunningJobs(user1, ImportJobCostTier.LARGE));
        System.out.println( "[--COUNT--] = " + redisStore.countRunningJobs(user1, ImportJobCostTier.XL));
        System.out.println( "[--COUNT--] = " + redisStore.countRunningJobs(user2, ImportJobCostTier.MEDIUM));
        System.out.println( "[--COUNT--] = " + redisStore.countRunningJobs(user2, ImportJobCostTier.SMALL));
    }
}
*/
