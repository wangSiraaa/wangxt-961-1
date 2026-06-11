package com.charging.shed.exception;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        log.error("业务异常: {}", e.getMessage());
        ErrorResponse error = new ErrorResponse(e.getCode(), e.getMessage(), LocalDateTime.now());
        return new ResponseEntity<>(error, HttpStatus.valueOf(e.getCode()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException e) {
        log.error("认证失败: {}", e.getMessage());
        ErrorResponse error = new ErrorResponse(401, "用户名或密码错误", LocalDateTime.now());
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
        log.error("访问拒绝: {}", e.getMessage());
        ErrorResponse error = new ErrorResponse(403, "权限不足", LocalDateTime.now());
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.error("参数校验失败: {}", errors);
        ErrorResponse error = new ErrorResponse(400, "参数校验失败", LocalDateTime.now(), errors);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        log.error("系统异常", e);
        ErrorResponse error = new ErrorResponse(500, "系统内部错误: " + e.getMessage(), LocalDateTime.now());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Data
    public static class ErrorResponse {
        private int code;
        private String message;
        private LocalDateTime timestamp;
        private Map<String, String> errors;

        public ErrorResponse(int code, String message, LocalDateTime timestamp) {
            this.code = code;
            this.message = message;
            this.timestamp = timestamp;
        }

        public ErrorResponse(int code, String message, LocalDateTime timestamp, Map<String, String> errors) {
            this.code = code;
            this.message = message;
            this.timestamp = timestamp;
            this.errors = errors;
        }
    }
}
