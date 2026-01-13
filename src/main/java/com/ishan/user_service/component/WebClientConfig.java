package com.ishan.user_service.component;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * This class is a Spring CONFIGURATION class.
 *
 * @Configuration tells Spring:
 * "This class contains bean definitions that should be managed by the Spring container."
 *
 * Why NOT @Component?
 * -------------------
 * - @Component is for regular beans (services, controllers, helpers).
 * - @Configuration is for classes whose PRIMARY job is to CREATE other beans.
 *
 * Internally:
 * - @Configuration uses CGLIB proxying
 * - Ensures @Bean methods return the SAME singleton instance
 * - Prevents accidental multiple bean creation
 *
 * In short:
 * @Configuration = "Factory class for beans"
 */
@Configuration
public class WebClientConfig {

    /**
     * @Bean tells Spring:
     * "The object returned by this method should be registered
     *  as a Spring-managed bean in the ApplicationContext."
     *
     * Bean name:
     * - By default, bean name = method name → "webClient"
     *
     * Lifecycle:
     * - Created at application startup
     * - Singleton by default
     * - Can be injected anywhere using constructor injection
     */
    @Bean
    public WebClient webClient() {

        /*
         * WebClient.builder()
         * -------------------
         * Creates a WebClient builder which allows us to configure:
         * - base URL
         * - codecs
         * - headers
         * - timeouts
         *
         * We use builder instead of WebClient.create()
         * because builder allows customization.
         */
        return WebClient.builder()

                /*
                 * baseUrl("https://randomuser.me")
                 * --------------------------------
                 * Sets a FIXED base URL for this WebClient.
                 *
                 * Benefit:
                 * - Avoids repeating the base URL in every request
                 * - Makes code cleaner and less error-prone
                 *
                 * Later we can just use:
                 * .uri("/api/")
                 */
                .baseUrl("https://randomuser.me")

                /*
                 * codecs(...)
                 * ------------
                 * Configures how request and response bodies are encoded/decoded.
                 *
                 * By default, WebClient buffers ONLY 256 KB in memory.
                 * Large responses (like 1000 users) exceed this limit.
                 *
                 * This customization increases the buffer size.
                 */
                .codecs(configurer ->
                        configurer.defaultCodecs()

                                /*
                                 * maxInMemorySize(10 * 1024 * 1024)
                                 * --------------------------------
                                 * Increases the maximum in-memory buffer size to 10 MB.
                                 *
                                 * Why needed:
                                 * - RandomUser API returns large JSON payloads
                                 * - Prevents DataBufferLimitException
                                 *
                                 * Important:
                                 * - This does NOT remove limits
                                 * - It just raises them in a controlled way
                                 */
                                .maxInMemorySize(10 * 1024 * 1024)
                )

                /*
                 * build()
                 * --------
                 * Finalizes the WebClient configuration
                 * and returns a fully constructed WebClient instance.
                 */
                .build();
    }
}
