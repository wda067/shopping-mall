package com.shop.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
public class OrderResponse {

    private final String email;
    private final String orderProduct;
    private final String totalAmount;
    private final String status;

    @Builder
    public OrderResponse(String email, String orderProduct, String totalAmount, String status) {
        this.email = email;
        this.orderProduct = orderProduct;
        this.totalAmount = totalAmount;
        this.status = status;
    }
}
