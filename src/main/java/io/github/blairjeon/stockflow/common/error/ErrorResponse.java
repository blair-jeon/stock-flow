package io.github.blairjeon.stockflow.common.error;

public record ErrorResponse(
        int status,
        String code,
        String message
) {
    public static ErrorResponse from(ErrorCode errorCode){
        return new ErrorResponse(
                errorCode.getHttpStatus().value(),
                errorCode.name(),
                errorCode.getMessage()
        );
    }
}