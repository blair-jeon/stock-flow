package io.github.blairjeon.stockflow.common.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_INPUT(
            HttpStatus.BAD_REQUEST,
            "Please check your input."
    ),
    INVALID_REQUEST(
            HttpStatus.BAD_REQUEST,
            "The request format is invalid."
    ),
    DUPLICATE_ORDER_ITEM(
            HttpStatus.BAD_REQUEST,
            "The order contains the same product more than once."
    ),
    WRONG_PASSWORD(
            HttpStatus.BAD_REQUEST,
            "The password is incorrect."
    ),
    LOGIN_REQUIRED(
            HttpStatus.UNAUTHORIZED,
            "Please log in first."
    ),
    LOGIN_FAILED(
            HttpStatus.UNAUTHORIZED,
            "The login ID or password is incorrect."
    ),
    INVALID_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "The token is invalid."
    ),
    TOKEN_EXPIRED(
            HttpStatus.UNAUTHORIZED,
            "The token has expired."
    ),
    TOKEN_USER_NOT_FOUND(
            HttpStatus.UNAUTHORIZED,
            "The user linked to this token no longer exists."
    ),
    ACCESS_DENIED(
            HttpStatus.FORBIDDEN,
            "You do not have permission."
    ),
    SELLER_ONLY(
            HttpStatus.FORBIDDEN,
            "Only sellers can do this."
    ),
    PRODUCT_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "The product was not found."
    ),
    ORDER_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "The order was not found."
    ),
    LOGIN_ID_TAKEN(
            HttpStatus.CONFLICT,
            "The login ID is already in use."
    ),
    PRODUCT_NOT_FOR_SALE(
            HttpStatus.CONFLICT,
            "The product is not for sale."
    ),
    NOT_ENOUGH_STOCK(
            HttpStatus.CONFLICT,
            "There is not enough stock."
    ),
    ORDER_CONFLICT(
            HttpStatus.CONFLICT,
            "Product details or stock changed. Please check your order."
    ),
    CANNOT_CANCEL_ORDER(
            HttpStatus.CONFLICT,
            "The order cannot be canceled in its current state."
    ),
    CANNOT_CHANGE_PRODUCT_STATUS(
            HttpStatus.CONFLICT,
            "This product status change is not allowed."
    ),
    SERVER_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Something went wrong on the server."
    ),
    SERVICE_UNAVAILABLE(
            HttpStatus.SERVICE_UNAVAILABLE,
            "The service is temporarily unavailable. Please try again later."
    );

    private final HttpStatus httpStatus;
    private final String message;
}