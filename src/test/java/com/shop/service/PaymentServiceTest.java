package com.shop.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.shop.client.TossPaymentsClient;
import com.shop.dto.request.PaymentRequest;
import com.shop.dto.response.CommonResponse;
import com.shop.dto.response.PaymentResponse;
import com.shop.event.OrderEventPublisher;
import com.shop.exception.CustomFeignException;
import com.shop.repository.PaymentRepository;
import com.shop.repository.order.OrderRepository;
import feign.FeignException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private TossPaymentsClient tossPaymentsClient;
    @Mock
    private PaymentTxService paymentTxService;

    @InjectMocks
    private PaymentService paymentService;

    private PaymentResponse buildResponse() {
        return new PaymentResponse(
                "pay_1234567890",
                "--MTIz",
                "테스트주문",
                "CARD",
                1000L,
                "DONE",
                "2025-11-06T00:00:00+09:00"
        );
    }

    @Test
    @DisplayName("외부 API 성공 시 정상 결제 처리")
    void confirmPayment_success() {
        //given
        PaymentRequest request = new PaymentRequest("pay_1234567890", "--MTIz", "1000");
        PaymentResponse response = buildResponse();

        given(tossPaymentsClient.confirmPayment(any())).willReturn(response);

        //when
        CommonResponse<PaymentResponse> result = paymentService.confirmPayment(request);

        //then
        assertEquals("200", result.getCode());
        assertEquals(response.getPaymentKey(), result.getBody().getPaymentKey());
        verify(paymentTxService).updateOrderPaymentKey(response);
        verify(paymentTxService).successPayment(response);
        verify(paymentTxService, never()).compensateOrder(any());
    }

    @Test
    @DisplayName("외부 API 실패 시 주문 보상 및 실패 응답 반환")
    void confirmPayment_apiFailure() {
        //given
        PaymentRequest request = new PaymentRequest("pay_1234567890", "--MTIz", "1000");
        given(tossPaymentsClient.confirmPayment(any()))
                .willThrow(new CustomFeignException("500", "외부 API 실패"));

        //when
        CommonResponse<PaymentResponse> result = paymentService.confirmPayment(request);

        //then
        assertEquals("500", result.getCode());
        assertNull(result.getBody());
        verify(paymentTxService).compensateOrder(anyLong());
        verify(paymentTxService, never()).successPayment(any());
    }

    @Test
    @DisplayName("외부 API 성공했으나 DB 저장 실패 시 예외 로그 출력 및 성공 응답 반환")
    void confirmPayment_dbFailure() {
        //given
        PaymentRequest request = new PaymentRequest("pay_1234567890", "--MTIz", "1000");
        PaymentResponse response = buildResponse();

        given(tossPaymentsClient.confirmPayment(any())).willReturn(response);
        willDoNothing().given(paymentTxService).updateOrderPaymentKey(response);
        willThrow(new RuntimeException("DB 저장 실패")).given(paymentTxService).successPayment(response);

        //when
        CommonResponse<PaymentResponse> result = paymentService.confirmPayment(request);

        //then
        assertEquals("200", result.getCode());
        assertEquals(response.getPaymentKey(), result.getBody().getPaymentKey());
        verify(paymentTxService).updateOrderPaymentKey(response);
        verify(paymentTxService).successPayment(response);
        verify(paymentTxService, never()).compensateOrder(any());
    }
}
