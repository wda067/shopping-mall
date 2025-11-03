package com.shop.dto.response;

import com.shop.domain.member.Member;
import com.shop.domain.order.Order;
import lombok.Getter;

@Getter
public class OrderPaymentInfo {

    private final String orderId;
    private final String orderName;
    private final Integer totalAmount;
    private final String customerEmail;
    private final String customerName;

    public OrderPaymentInfo(Order order, Member member) {
        this.orderId = String.valueOf(order.getId());
        this.orderName = order.getOrderName();
        this.totalAmount = order.getTotalAmount();
        this.customerEmail = member.getEmail();
        this.customerName = member.getName();
    }
}