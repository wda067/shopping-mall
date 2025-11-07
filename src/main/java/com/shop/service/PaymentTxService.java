package com.shop.service;

import com.shop.domain.order.Order;
import com.shop.domain.payment.Payment;
import com.shop.dto.response.PaymentResponse;
import com.shop.event.OrderEventPublisher;
import com.shop.exception.OrderNotFound;
import com.shop.repository.PaymentRepository;
import com.shop.repository.order.OrderRepository;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentTxService {

    private static final Logger orderLogger = LoggerFactory.getLogger("OrderLogger");

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderEventPublisher orderEventPublisher;

    //결제 성공 -> DB 저장
    public void successPayment(PaymentResponse response) {
        Order order = orderRepository.findById(decodeOrderId(response))
                .orElseThrow(OrderNotFound::new);
        Payment payment = new Payment(response, order);
        paymentRepository.save(payment);

        orderEventPublisher.publishOrderCompleted(order);
        orderLogger.info("주문 완료");
    }

    //주문에 PaymentKey 우선 저장
    public void updateOrderPaymentKey(PaymentResponse response) {
        Order order = orderRepository.findById(decodeOrderId(response))
                .orElseThrow(OrderNotFound::new);
        order.setPaymentKey(response.getPaymentKey());
    }

    //결제 내역 보상
    public void saveRecoveredPayment(PaymentResponse response) {
        Order order = orderRepository.findById(decodeOrderId(response))
                .orElseThrow(OrderNotFound::new);
        Payment payment = new Payment(response, order);
        paymentRepository.save(payment);
        orderEventPublisher.publishOrderCompleted(order);
        orderLogger.info("주문 완료");
    }

    //결제 실패 시 주문 취소
    public void compensateOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(OrderNotFound::new);
        order.cancel();
    }

    private Long decodeOrderId(PaymentResponse response) {
        String cleaned = response.getOrderId().replaceAll("-", "");
        byte[] decode = Base64.getDecoder().decode(cleaned);
        String rawOrderId = new String(decode);
        return Long.parseLong(rawOrderId);
    }
}
