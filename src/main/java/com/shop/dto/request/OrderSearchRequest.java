package com.shop.dto.request;

import com.shop.domain.order.OrderStatus;
import jakarta.validation.constraints.AssertTrue;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

@Getter
@Setter
public class OrderSearchRequest {

    @DateTimeFormat(iso = ISO.DATE_TIME)
    private LocalDateTime startDate;

    @DateTimeFormat(iso = ISO.DATE_TIME)
    private LocalDateTime endDate;

    private Integer year;

    private OrderStatus status;

    private int minAmount;

    @AssertTrue(message = "유효하지 않은 기간입니다. 시작일은 종료일 이전이어야 합니다.")
    public boolean isValidRangeOrder() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return startDate.isBefore(endDate);
    }
}
