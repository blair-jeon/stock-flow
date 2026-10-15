package io.github.blairjeon.stockflow.common.error;

import java.util.List;

public record ErrorResponse(
        int status,
        String code,
        String message,
        List<FieldErrorResponse> fieldErrors
) {
    public static ErrorResponse from(ErrorCode errorCode){
        return from(errorCode, List.of());
    }

    public static ErrorResponse from(ErrorCode errorCode, List<FieldErrorResponse> fieldErrors){
        return new ErrorResponse(
                errorCode.getHttpStatus().value(),
                errorCode.name(),
                errorCode.getMessage(),
                fieldErrors
        );
    }
}