package com.shop.exception;

import static com.shop.exception.ErrorCode.PAYMENT_NOT_FOUND;

public class PaymentNotFound extends CustomException {

    public PaymentNotFound() {
        super(PAYMENT_NOT_FOUND);
    }
}
