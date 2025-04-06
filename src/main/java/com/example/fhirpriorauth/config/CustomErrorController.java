package com.example.fhirpriorauth.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Custom error controller to provide simple error messages
 */
@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    @ResponseBody
    public ResponseEntity<String> handleError(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute("jakarta.servlet.error.status_code");
        Exception exception = (Exception) request.getAttribute("jakarta.servlet.error.exception");
        
        if (statusCode == null) {
            statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
        }
        
        String errorMessage;
        if (statusCode == HttpStatus.NOT_FOUND.value()) {
            errorMessage = "Error - Resource not found";
        } else if (statusCode == HttpStatus.METHOD_NOT_ALLOWED.value()) {
            errorMessage = "Error - Method not allowed";
        } else if (exception != null) {
            errorMessage = "Error - " + exception.getMessage();
        } else {
            errorMessage = "Error - " + HttpStatus.valueOf(statusCode).getReasonPhrase();
        }
        
        return ResponseEntity.status(statusCode).body(errorMessage);
    }
}
