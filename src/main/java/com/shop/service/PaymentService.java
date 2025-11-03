package com.shop.service;

import com.shop.client.TossPaymentsClient;
import com.shop.domain.member.Member;
import com.shop.domain.order.Order;
import com.shop.domain.payment.Payment;
import com.shop.dto.request.PaymentRequest;
import com.shop.dto.response.CommonResponse;
import com.shop.dto.response.OrderPaymentInfo;
import com.shop.dto.response.PaymentResponse;
import com.shop.event.OrderEventPublisher;
import com.shop.exception.OrderNotFound;
import com.shop.repository.order.OrderRepository;
import com.shop.repository.PaymentRepository;
import java.util.Base64;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Logger orderLogger = LoggerFactory.getLogger("OrderLogger");

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final TossPaymentsClient tossPaymentsClient;
    private final OrderEventPublisher orderEventPublisher;

    public OrderPaymentInfo getOrderPaymentInfo(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(OrderNotFound::new);
        Member member = order.getMember();
        order.processPayment();

        return new OrderPaymentInfo(order, member);
    }

    @Transactional
    public CommonResponse<PaymentResponse> confirmPayment(PaymentRequest request) {
        orderLogger.info("결제 승인 요청");
        PaymentResponse response = tossPaymentsClient.confirmPayment(request);
        Order order = orderRepository.findById(decodeOrderId(response))
                .orElseThrow(OrderNotFound::new);

        Payment payment = new Payment(response, order);
        paymentRepository.save(payment);

        //주문 완료 이벤트 발행
        orderEventPublisher.publishOrderCompleted(order);
        orderLogger.info("주문 완료");
        return CommonResponse.success(response);
    }

    private Long decodeOrderId(PaymentResponse response) {
        String cleaned = response.getOrderId().replaceAll("-", "");
        byte[] decode = Base64.getDecoder().decode(cleaned);
        String rawOrderId = new String(decode);
        return Long.parseLong(rawOrderId);
    }

    @Transactional
    public CommonResponse<List<PaymentResponse>> findAll() {
        return CommonResponse.success(paymentRepository.findAll().stream()
                .map(PaymentResponse::new)
                .toList());
    }
}
