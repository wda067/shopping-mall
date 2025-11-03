package com.shop.global.feign;

import feign.Logger;
import feign.Retryer;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TossPaymentsFeignConfig {

    @Bean
    public Encoder feignEncoder() {
        return new CustomFeignEncoder();
    }

    @Bean
    public Decoder feignDecoder() {
        return new CustomFeignDecoder();
    }

    @Bean
    public ErrorDecoder feignErrorDecoder() {
        return new CustomFeignErrorDecoder();
    }

    @Bean
    public CustomFeignRequestInterceptor customFeignRequestInterceptor() {
        return new CustomFeignRequestInterceptor();
    }

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;  //요청 및 응답 모든 로그 출력
    }

    @Bean
    public Retryer feignRetryer() {
        return new Retryer.Default(100, TimeUnit.MICROSECONDS.toMillis(500), 3);
    }
}

