package com.shop.repository.order.dto;

import java.time.LocalDateTime;

public interface OrderStatisticsProjection {

    String getEmail();
    Long getTotalOrders();
    Long getTotalAmount();
    Double getAverageAmount();
    LocalDateTime getLastOrderDate();
}
