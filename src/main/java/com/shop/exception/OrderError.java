package com.shop.exception;

import static com.shop.exception.ErrorCode.ORDER_ERROR;

public class OrderError extends CustomException {

    public OrderError() {
        super(ORDER_ERROR);
    }
}
