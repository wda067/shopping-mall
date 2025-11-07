package com.shop.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.shop.client.TossPaymentsClient;
import com.shop.domain.member.Member;
import com.shop.domain.order.Order;
import com.shop.domain.order.OrderProduct;
import com.shop.domain.product.Product;
import com.shop.dto.response.PaymentResponse;
import com.shop.repository.order.OrderRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentRecoveryServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private TossPaymentsClient tossPaymentsClient;

    @Mock
    private PaymentTxService paymentTxService;

    @InjectMocks
    private PaymentRecoveryService paymentRecoveryService;

    private Order testOrder;

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

    @BeforeEach
    void setUp() {
        Member member = Member.builder()
                .email("test@test.com")
                .name("테스트")
                .build();
        List<OrderProduct> orderProducts = new ArrayList<>();
        orderProducts.add(new OrderProduct(Product.builder().build(), 5));
        testOrder = new Order(member, orderProducts);
        testOrder.setPaymentKey("pay_1234567890");
    }

    @Test
    @DisplayName("보정 대상 주문이 없으면 아무 작업도 수행하지 않음")
    void schedulePaymentRecovery_NoMissingOrders() {
        //given
        doReturn(Collections.emptyList())
                .when(orderRepository).findPaymentPendingOrdersWithoutPayment();

        //when
        paymentRecoveryService.schedulePaymentRecovery();

        //then
        verify(orderRepository, never()).findById(any());
        verify(tossPaymentsClient, never()).getPaymentByPaymentKey(any());
        verify(paymentTxService, never()).saveRecoveredPayment(any());
    }

    @Test
    @DisplayName("보정 대상 주문이 존재하면 결제 내역 보정 수행")
    void schedulePaymentRecovery_WithMissingOrders() {
        //given
        List<Long> orderIds = List.of(1L, 2L);
        doReturn(orderIds)
                .when(orderRepository).findPaymentPendingOrdersWithoutPayment();

        doReturn(Optional.of(testOrder))
                .when(orderRepository).findById(anyLong());

        PaymentResponse response = buildResponse();
        doReturn(response)
                .when(tossPaymentsClient).getPaymentByPaymentKey(anyString());

        //when
        paymentRecoveryService.schedulePaymentRecovery();

        //then
        verify(orderRepository, times(orderIds.size())).findById(anyLong());
        verify(tossPaymentsClient, times(orderIds.size())).getPaymentByPaymentKey(eq(testOrder.getPaymentKey()));
        verify(paymentTxService, times(orderIds.size())).saveRecoveredPayment(eq(response));
    }

    //@Test
    //@DisplayName("외부 API 호출 실패 시에도 예외 전파 없이 로그만 남김")
    //void recoverMissingPayment_ApiFailure() {
    //    //given
    //    doReturn(Optional.of(testOrder))
    //            .when(orderRepository).findById(1L);
    //
    //    doThrow(new RuntimeException("API 호출 실패"))
    //            .when(tossPaymentsClient).getPaymentByPaymentKey(anyString());
    //
    //    //when
    //    paymentRecoveryService.recoverMissingPayment(1L);
    //
    //    //then
    //    verify(paymentTxService, never()).saveRecoveredPayment(any());
    //}
    //
    //@Test
    //@DisplayName("정상 케이스 - 외부 API 응답 성공 시 PaymentTxService 호출")
    //void recoverMissingPayment_Success() {
    //    // given
    //    doReturn(Optional.of(testOrder))
    //            .when(orderRepository).findById(1L);
    //
    //    PaymentResponse response = buildResponse();
    //    doReturn(response)
    //            .when(tossPaymentsClient).getPaymentByPaymentKey(anyString());
    //
    //    // when
    //    paymentRecoveryService.recoverMissingPayment(1L);
    //
    //    // then
    //    verify(paymentTxService, times(1)).saveRecoveredPayment(eq(response));
    //}
}
