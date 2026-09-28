package com.orderhub.api;

import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail transition(IllegalStateException ex) { return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage()); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail constraint(DataIntegrityViolationException ex) { return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Resource conflicts with existing data"); }
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail concurrent(OptimisticLockingFailureException ex) { return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Order was changed concurrently; reload and retry"); }
}
