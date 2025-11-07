package com.shop.service;

import com.shop.client.TossPaymentsClient;
import com.shop.domain.member.Member;
import com.shop.domain.order.Order;
import com.shop.dto.request.PaymentRequest;
import com.shop.dto.response.CommonResponse;
import com.shop.dto.response.OrderPaymentInfo;
import com.shop.dto.response.PaymentResponse;
import com.shop.event.OrderEventPublisher;
import com.shop.exception.CustomFeignException;
import com.shop.exception.OrderNotFound;
import com.shop.repository.PaymentRepository;
import com.shop.repository.order.OrderRepository;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Logger orderLogger = LoggerFactory.getLogger("OrderLogger");

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final TossPaymentsClient tossPaymentsClient;
    private final PaymentTxService paymentTxService;
    private final EmailService emailService;
    private final OrderEventPublisher orderEventPublisher;

    public OrderPaymentInfo getOrderPaymentInfo(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(OrderNotFound::new);
        Member member = order.getMember();
        order.processPayment();

        return new OrderPaymentInfo(order, member);
    }

    public CommonResponse<PaymentResponse> confirmPayment(PaymentRequest request) {
        long start = System.currentTimeMillis();
        try {
            orderLogger.info("========== [결제 승인 요청 시작] ==========");

            PaymentResponse response;
            String message = "";

            //토스페이먼츠 API 요청
            try {
                response = tossPaymentsClient.confirmPayment(request);
                paymentTxService.updateOrderPaymentKey(response);  //paymentKey 저장
                orderLogger.info("토스페이먼츠 API 결제 요청 성공");
            } catch (CustomFeignException e) {  //토스페이먼츠 API 요청 실패 -> 주문 취소
                message = e.getMessage();
                orderLogger.error("토스페이먼츠 API 결제 요청 실패: {}", e.getMessage());
                paymentTxService.compensateOrder(decodeOrderId(request.getOrderId()));  //주문 취소
                return CommonResponse.fail(e.getCode(), message);
            }

            //토스페이먼츠 API 요청 성공 -> 결제 성공
            try {
                paymentTxService.successPayment(response);  //주문 성공 & 결제 내역 저장
                orderLogger.info("결제 성공");
                return CommonResponse.success(response);
            } catch (Exception e) {  //결제 내역 DB 저장 실패 -> 보정 필요
                orderLogger.error("결제 내역 DB 저장 실패. 보정 필요: {}", e.getMessage());
            }

            return CommonResponse.success(response);
        } finally {
            long end = System.currentTimeMillis();
            orderLogger.info("========== [결제 승인 요청 종료] 전체 처리 시간: {}ms ==========", (end - start));
        }
    }

    private Long decodeOrderId(String orderId) {
        String cleaned = orderId.replaceAll("-", "");
        byte[] decode = Base64.getDecoder().decode(cleaned);
        String rawOrderId = new String(decode);
        return Long.parseLong(rawOrderId);
    }
}
