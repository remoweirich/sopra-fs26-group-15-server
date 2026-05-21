package ch.uzh.ifi.hase.soprafs26.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionAdviceTest {

    private final GlobalExceptionAdvice advice = new GlobalExceptionAdvice();

    @Test
    void handleConflict_illegalArgument_returnsConflict() {
        IllegalArgumentException ex = new IllegalArgumentException("bad arg");
        WebRequest request = new ServletWebRequest(new MockHttpServletRequest());

        ResponseEntity<Object> response = advice.handleConflict(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void handleTransactionSystemException_returnsConflict() {
        HttpServletRequest request = new MockHttpServletRequest();
        Exception ex = new Exception("tx error");

        ResponseStatusException result = advice.handleTransactionSystemException(ex, request);

        assertEquals(HttpStatus.CONFLICT, result.getStatusCode());
    }

    @Test
    void handleException_returnsInternalServerError() {
        Exception ex = new Exception("server error");

        ResponseStatusException result = advice.handleException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
    }
}