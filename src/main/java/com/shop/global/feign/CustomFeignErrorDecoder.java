package com.shop.global.feign;

import com.shop.exception.CustomFeignException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomFeignErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        log.error("[Feign ErrorDecoder] Error occurred - Method: {}, Status: {}, Reason: {}",
                methodKey, response.status(), response.reason());

        return new CustomFeignException(String.valueOf(response.status()), response.reason());
    }
}
