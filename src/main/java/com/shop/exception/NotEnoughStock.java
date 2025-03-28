package com.shop.exception;


import static com.shop.exception.ErrorCode.NOT_ENOUGH_STOCK;

public class NotEnoughStock extends CustomException {

    public NotEnoughStock() {
        super(NOT_ENOUGH_STOCK);
    }
}
