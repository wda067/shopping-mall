package com.shop.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderCreateRequest {

    @NotNull
    private Long productId;

    @Min(value = 1, message = "주문 수량은 1개 이상이어야 합니다.")
    private int quantity;
}
