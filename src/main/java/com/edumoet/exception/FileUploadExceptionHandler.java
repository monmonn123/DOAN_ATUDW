package com.edumoet.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler cho file upload errors
 */
@RestControllerAdvice
public class FileUploadExceptionHandler {
    
    @ExceptionHandler(IOException.class)
    public ResponseEntity<?> handleIOException(IOException e) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "File upload failed: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException e) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Invalid input: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
