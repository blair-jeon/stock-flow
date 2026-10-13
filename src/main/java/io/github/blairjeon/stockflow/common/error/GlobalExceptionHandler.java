package io.github.blairjeon.stockflow.common.error;

import io.github.blairjeon.stockflow.auth.exception.InvalidCredentialsException;
import io.github.blairjeon.stockflow.order.exception.DuplicateOrderItemException;
import io.github.blairjeon.stockflow.order.exception.OrderNotFoundException;
import io.github.blairjeon.stockflow.order.exception.OrderPermissionDeniedException;
import io.github.blairjeon.stockflow.product.exception.ProductNotFoundException;
import io.github.blairjeon.stockflow.product.exception.SellerRequiredException;
import io.github.blairjeon.stockflow.security.jwt.InvalidTokenException;
import io.github.blairjeon.stockflow.user.exception.DuplicateLoginIdException;
import io.github.blairjeon.stockflow.user.exception.PasswordMismatchException;
import io.github.blairjeon.stockflow.user.exception.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            PasswordMismatchException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException e){
        ErrorResponse response = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler({
            SellerRequiredException.class,
            OrderPermissionDeniedException.class
    })
    public ResponseEntity<ErrorResponse> handleForbidden(RuntimeException e){
        ErrorResponse response = new ErrorResponse(HttpStatus.FORBIDDEN.value(), e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler({
            InvalidCredentialsException.class,
            InvalidTokenException.class
    })
    public ResponseEntity<ErrorResponse> handleUnauthorized(RuntimeException e){
        ErrorResponse response = new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler({
            UserNotFoundException.class,
            ProductNotFoundException.class,
            OrderNotFoundException.class,
    })
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException e){
        ErrorResponse response = new ErrorResponse(HttpStatus.NOT_FOUND.value(), e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler({
            DuplicateLoginIdException.class,
            DuplicateOrderItemException.class
    })
    public ResponseEntity<ErrorResponse> handleConflict(RuntimeException e){
        ErrorResponse response = new ErrorResponse(HttpStatus.CONFLICT.value(), e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
}