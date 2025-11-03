package com.shop.global.feign;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.exception.CustomFeignException;
import feign.RequestTemplate;
import feign.codec.Encoder;
import java.lang.reflect.Type;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomFeignEncoder implements Encoder {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void encode(Object object, Type bodyType, RequestTemplate template) {
        try {
            String jsonBody = objectMapper.writeValueAsString(object);
            log.info("[Feign Encoder] Request Body: {}", jsonBody);
            template.body(jsonBody.getBytes(), template.requestCharset());
        } catch (Exception e) {
            log.error("[Feign Encoder] Encoding error: ", e);
            throw new CustomFeignException("400", e.getMessage());
        }
    }
}
