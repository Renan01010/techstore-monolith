package com.techstore.shared.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void returnsValidationProblemWithoutRejectedValue() {
        Object target = new Object();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "request");
        bindingResult.addError(new FieldError("request", "email", "not-an-email", false,
                null, null, "must be a valid email"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                null, bindingResult);
        MockHttpServletRequest request = requestWithCorrelationId("corr-456");

        ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(
                exception,
                HttpHeaders.EMPTY,
                HttpStatus.BAD_REQUEST,
                new ServletWebRequest(request));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getHeaders().getContentType()).isEqualTo(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON);
        ProblemDetail problem = (ProblemDetail) response.getBody();
        assertThat(problem).isNotNull();
        assertThat(problem.getTitle()).isEqualTo("Request validation failed");
        assertThat(problem.getProperties()).containsEntry("correlationId", "corr-456");
        assertThat(problem.getProperties().get("errors").toString())
                .contains("email", "must be a valid email")
                .doesNotContain("not-an-email");
    }

    @Test
    void unexpectedErrorDoesNotExposeExceptionDetails() {
        MockHttpServletRequest request = requestWithCorrelationId("corr-789");

        ResponseEntity<ProblemDetail> response = handler.handleUnexpectedException(
                new IllegalStateException("sensitive database detail"),
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).doesNotContain("sensitive");
        assertThat(response.getBody().getProperties()).containsEntry("correlationId", "corr-789");
    }

    private static MockHttpServletRequest requestWithCorrelationId(String id) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/example");
        request.setAttribute(CorrelationIdFilter.MDC_KEY, id);
        return request;
    }
}
