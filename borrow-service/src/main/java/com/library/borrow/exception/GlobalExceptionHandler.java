package com.library.borrow.exception;

import com.library.borrow.common.ApiResponse;
import feign.FeignException;
import feign.codec.DecodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> metadata = new HashMap<>();
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        metadata.put("validationErrors", fieldErrors);
        return ResponseEntity.badRequest().body(ApiResponse.error("VALIDATION_ERROR", "Invalid request body.", metadata));
    }

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApiResponse<Void>> handleApp(ApplicationException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.error(ex.getCode(), ex.getMessage(), Map.of()));
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiResponse<Void>> handleFeign(FeignException ex) {
        log.warn("Downstream HTTP error from Feign client: status={}", ex.status(), ex);
        String message = "A dependent service returned an error (HTTP " + ex.status() + ").";
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.error("DOWNSTREAM_ERROR", message, Map.of("status", ex.status())));
    }

    @ExceptionHandler(DecodeException.class)
    public ResponseEntity<ApiResponse<Void>> handleFeignDecode(DecodeException ex) {
        log.warn("Feign failed to decode downstream response: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.error("DOWNSTREAM_DECODE_ERROR", "Could not read response from a dependent service.", Map.of()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_ERROR", "Unexpected server error.", Map.of()));
    }
}
