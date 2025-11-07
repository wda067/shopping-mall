package com.shop.service;

import com.shop.client.TossPaymentsClient;
import com.shop.domain.order.Order;
import com.shop.dto.response.PaymentResponse;
import com.shop.exception.OrderNotFound;
import com.shop.repository.order.OrderRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentRecoveryService {

    private static final Logger orderLogger = LoggerFactory.getLogger("OrderLogger");

    private final OrderRepository orderRepository;
    private final TossPaymentsClient tossPaymentsClient;
    private final PaymentTxService paymentTxService;

    @Scheduled(cron = "0 */5 * * * *")
    public void schedulePaymentRecovery() {
        orderLogger.info("결제 내역 보정 스케줄링 시작");

        //결제 대기중
        List<Long> missingPaymentOrderIds = orderRepository.findPaymentPendingOrdersWithoutPayment();

        if (missingPaymentOrderIds.isEmpty()) {
            orderLogger.info("보정할 결제 내역 없음");
            return;
        }

        orderLogger.warn("결제 내역 누락 주문 발견: {}건", missingPaymentOrderIds.size());

        for (Long orderId : missingPaymentOrderIds) {
            recoverMissingPayment(orderId);
        }
    }

    public void recoverMissingPayment(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(OrderNotFound::new);

        //토스페이먼츠 API 결제 조회 요청
        try {
            PaymentResponse response = tossPaymentsClient.getPaymentByPaymentKey(order.getPaymentKey());
            paymentTxService.saveRecoveredPayment(response);  //결제 내역 DB 반영
            orderLogger.info("결제 내역 보정 성공: orderId={}", orderId);
        } catch (Exception e) {
            orderLogger.error("결제 내역 보정 실패: orderId={}, msg={}", orderId, e.getMessage());
        }
    }
}
