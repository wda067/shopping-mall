package com.shop.controller;

import com.shop.dto.response.ErrorResponse;
import com.shop.exception.ErrorCode;
import com.shop.exception.CustomException;
import com.shop.exception.CustomFeignException;
import com.shop.exception.Unauthorized;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExceptionController {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorResponse exceptionHandler(MethodArgumentNotValidException exception) {
        return new ErrorResponse(ErrorCode.BAD_REQUEST, exception.getFieldErrors());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(CustomException.class)
    public ErrorResponse exceptionHandler(CustomException e) {
        return new ErrorResponse(e.getErrorCode());
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(Unauthorized.class)
    public ErrorResponse exceptionHandler(Unauthorized e) {
        return new ErrorResponse(e.getErrorCode());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(CustomFeignException.class)
    public ErrorResponse exceptionHandler(CustomFeignException e) {
        return new ErrorResponse(e.getCode(), e.getMessage());
    }
}
