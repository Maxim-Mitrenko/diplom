package com.example.diplom.controller;

import com.example.diplom.exeption.FileNotFoundException;
import com.example.diplom.exeption.UserNotFoundException;
import com.example.diplom.model.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class ExceptionController {

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<ErrorResponse> fileNotFound(FileNotFoundException ex) {
        log.warn(ex.toString());
        return new ResponseEntity<>(new ErrorResponse(ex.toString(), 400), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> userNotFound(UserNotFoundException ex) {
        log.warn(ex.toString());
        return new ResponseEntity<>(new ErrorResponse(ex.toString(), 400), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ErrorResponse> error(Throwable error) {
        log.warn(error.toString());
        return new ResponseEntity<>(new ErrorResponse(error.toString(), 500), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
