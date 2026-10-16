package com.techstore.shared.adapter.in.web;

import java.net.URI;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String CORRELATION_ID_PROPERTY = "correlationId";

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        HttpServletRequest servletRequest = ((ServletWebRequest) request).getRequest();
        ProblemDetail problem = createProblem(HttpStatus.BAD_REQUEST,
                "Request validation failed", "One or more request fields are invalid.",
                servletRequest);
        List<ValidationError> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ValidationError(error.getField(), safeMessage(error)))
                .toList();
        problem.setProperty("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .headers(headers)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request) {
        logger.error("Unhandled request failure correlationId={} exceptionType={}",
                    request.getAttribute(CorrelationIdFilter.MDC_KEY), exception.getClass().getName());
        ProblemDetail problem = createProblem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                "The request could not be completed.",
                request);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private static ProblemDetail createProblem(
            HttpStatus status,
            String title,
            String detail,
            HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty(CORRELATION_ID_PROPERTY, request.getAttribute(CorrelationIdFilter.MDC_KEY));
        return problem;
    }

    private static String safeMessage(FieldError error) {
        String message = error.getDefaultMessage();
        return message == null || message.isBlank() ? "Invalid value." : message;
    }

    private record ValidationError(String field, String message) {
    }
}
