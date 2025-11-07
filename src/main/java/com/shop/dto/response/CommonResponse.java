package com.shop.dto.response;

import lombok.Getter;

@Getter
public class CommonResponse<T> {

    private final String code;
    private final String message;
    private final T body;

    public CommonResponse(String code, String message, T body) {
        this.code = code;
        this.message = message;
        this.body = body;
    }

    public static <T> CommonResponse<T> success(T body) {
        return new CommonResponse<T>(
                "200",
                "요청이 성공적으로 처리되었습니다.",
                body);
    }

    public static <T> CommonResponse<T> fail(String message) {
        return new CommonResponse<>(
                "500",
                message,
                null
        );
    }

    public static <T> CommonResponse<T> fail(String code, String message) {
        return new CommonResponse<>(
                code,
                message,
                null
        );
    }
}
