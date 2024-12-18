package com.seuprojeto.projeto_web.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({ EntityNotFoundException.class, TableEmptyException.class, DuplicateRegisterException.class, FieldInvalidException.class })
    public ResponseEntity<Map<String, String>> handleAllExceptions(Exception exception) {
        Map<String, String> errorResponse = new HashMap<>();

        if (exception instanceof EntityNotFoundException) {
            errorResponse.put("message", exception.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }

        if(exception instanceof TableEmptyException){
            errorResponse.put("message", exception.getMessage());
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(errorResponse);
        }

        if(exception instanceof DuplicateRegisterException){
            errorResponse.put("message", exception.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
        }

        if(exception instanceof FieldInvalidException){
            errorResponse.put("message", exception.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errorResponse = new HashMap<>();

        FieldError fieldError = ex.getBindingResult().getFieldError();
        if (fieldError != null) {
            errorResponse.put("field", fieldError.getField());
            errorResponse.put("message", fieldError.getDefaultMessage());
        } else {
            errorResponse.put("message", "Erro de validação desconhecido");
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

}
