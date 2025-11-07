package com.shop.global.feign;

import static java.nio.charset.StandardCharsets.UTF_8;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
public class CustomFeignRequestInterceptor implements RequestInterceptor {

    @Value("${toss-payments.widget-secret}")
    private String widgetSecretKey;

    @Override
    public void apply(RequestTemplate template) {
        String authHeader = "Basic " + Base64.getEncoder().encodeToString((widgetSecretKey + ":").getBytes(UTF_8));
        template.header("Authorization", authHeader);
        template.header("Content-Type", "application/json");
        //String requestId = MDC.get("requestId");
        //if (requestId != null) {
        //    template.header("X-Request-Id", requestId);
        //}
        log.info("[Feign RequestInterceptor] Request Method: {}, URL: {}", template.method(), template.url());
    }
}

