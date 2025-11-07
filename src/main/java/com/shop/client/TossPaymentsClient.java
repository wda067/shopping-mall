package com.shop.client;

import com.shop.global.feign.TossPaymentsFeignConfig;
import com.shop.dto.request.PaymentRequest;
import com.shop.dto.response.PaymentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "tossPaymentsClient",
        url = "https://api.tosspayments.com/v1/payments",
        configuration = TossPaymentsFeignConfig.class
)
public interface TossPaymentsClient {

    @PostMapping("/confirm")
    PaymentResponse confirmPayment(@RequestBody PaymentRequest request);

    @GetMapping("/orders/{orderId}")
    PaymentResponse getPayment(@PathVariable("orderId") String orderId);

    @GetMapping("/{paymentKey}")
    PaymentResponse getPaymentByPaymentKey(@PathVariable("paymentKey") String paymentKey);
}
