package com.shop.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderStatisticsResponse {

    private String email;
    private Long totalOrders;
    private Long totalAmount;
    private Double averageAmount;
    private LocalDateTime lastOrderDate;
}
