package org.app.athena.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

@ControllerAdvice
public class WebControllerAdvice {

    @ExceptionHandler(Exception.class) // Handle all other unexpected exceptions
    public ResponseEntity<Map<String,String>> handleGenericException(Exception ex) {
        return ResponseEntity.internalServerError().body (Map
                .of("Message",ex.getLocalizedMessage(),
                        "Type",ex.getClass().getName()
                        ));
    }

}
