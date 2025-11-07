package com.shop.event;

import com.shop.exception.EmailSendFailure;
import com.shop.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class OrderCompletedEventListener {

    private static final Logger orderLogger = LoggerFactory.getLogger("OrderLogger");

    private final EmailService emailService;

    //@Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCompletedEvent(OrderCompletedEvent event) {
        try {
            emailService.sendOrderConfirmation(event.getEmail(), event.getOrderName());
        } catch (EmailSendFailure e) {
            orderLogger.error("code: {}, message: {}", e.getErrorCode().getCode(), e.getErrorCode().getMessage());
        }
    }
}
