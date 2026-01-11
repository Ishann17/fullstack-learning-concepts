package com.ishan.user_service.exceptionHandler;

import com.ishan.user_service.customExceptions.UserNotFoundException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @RestControllerAdvice is used to handle exceptions globally across
 * all REST controllers in the application.
 * This ensures:
 * - Consistent error responses
 * - No try-catch blocks inside controllers
 * - Clean separation of concerns
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Inside @RestControllerAdvice, we use: @ExceptionHandler(SomeException.class) “When this exception occurs, handle it HERE.”
     * Handles JSON parsing / processing errors.
     * This exception typically occurs when:
     * - JSON structure is invalid
     * - Response format is not as expected
     * - ObjectMapper fails to parse JSON
     * We return HTTP 400 (Bad Request) because:
     * - The request could not be processed correctly
     * - The server understood the request but failed to parse data
     */
    @ExceptionHandler(JsonProcessingException.class)
    public ResponseEntity<?> handleJsonError(JsonProcessingException ex) {

        /*
         * We return a structured error response instead of a plain string
         */
        Map<String, Object> errorResponse = new LinkedHashMap<>();

        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "Invalid JSON format");
        errorResponse.put("message", ex.getOriginalMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<?> handleUserNotFoundException(UserNotFoundException exception){

        Map<String, Object> errorResponse = new LinkedHashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.NOT_FOUND.value());
        errorResponse.put("error", "User Not Found");
        errorResponse.put("message", exception.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);

    }
}
/* *******IMPORTANT NOTE*********
Controllers don’t call exception handlers directly.
When an exception is thrown during request processing,
Spring resolves it by finding a matching @ExceptionHandler,
typically defined in a @RestControllerAdvice,
and uses that to generate the HTTP response.”
 */