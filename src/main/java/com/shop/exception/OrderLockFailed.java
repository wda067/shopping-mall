package com.shop.exception;


import static com.shop.exception.ErrorCode.ORDER_LOCK_FAILED;

public class OrderLockFailed extends CustomException {

    public OrderLockFailed() {
        super(ORDER_LOCK_FAILED);
    }
}
