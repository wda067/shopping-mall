package com.shop.dto.response;

import com.shop.domain.order.OrderStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderResponse {

    private String email;
    private String orderNumber;
    private String orderName;
    private OrderStatus status;
    private LocalDateTime orderDate;
    private Long totalAmount;
    private int totalQuantity;
}
