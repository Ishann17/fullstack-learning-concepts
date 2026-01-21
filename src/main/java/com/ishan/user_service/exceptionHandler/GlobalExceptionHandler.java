package com.ishan.user_service.exceptionHandler;

import com.ishan.user_service.customExceptions.BatchLimitExceededException;
import com.ishan.user_service.customExceptions.UserNotFoundException;
import com.ishan.user_service.dto.UserDto;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

    @ExceptionHandler(BatchLimitExceededException.class)
    public ResponseEntity<?> handleBatchLimitExceededException(BatchLimitExceededException exception){
        Map<String, Object> errorResponse = new LinkedHashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "Batch count limit exceeded!");
        errorResponse.put("message", exception.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception){

        // Map to store field-specific errors
        // Key = field name (e.g., "firstName")
        // Value = error message (e.g., "First Name is required")
       Map<String, String> fieldErrors = new LinkedHashMap<>();

        /*
         Extracting field errors from the exception
         exception.getBindingResult() contains ALL validation failures.
        getFieldErrors() gives list of FieldError objects.
        Each FieldError contains:
        - which field failed (firstName/email/age)
        - which message should be shown (from annotation message)
        */
        exception.getBindingResult().getFieldErrors().forEach(error -> {
            // error.getField() = "firstName", "email", etc.
            // error.getDefaultMessage() = the message from @NotBlank, @Size, etc.
             fieldErrors.put(error.getField(), error.getDefaultMessage()); // overrides existing message
            //fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()); // keeps first/best message only
        });

        Map<String, Object> errorResponse = new LinkedHashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "Invalid Input in User Details");
        errorResponse.put("message", "Validation Failed");
        errorResponse.put("fieldErrors", fieldErrors);


        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

}
/* *******IMPORTANT NOTE*********
Controllers don’t call exception handlers directly.
When an exception is thrown during request processing,
Spring resolves it by finding a matching @ExceptionHandler,
typically defined in a @RestControllerAdvice,
and uses that to generate the HTTP response.”
 */