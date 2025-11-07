package com.shop.controller;

import com.shop.dto.request.OrderCreateRequest;
import com.shop.dto.request.OrderSearchRequest;
import com.shop.dto.response.CommonResponse;
import com.shop.dto.response.OrderResponse;
import com.shop.dto.response.OrderStatisticsResponse;
import com.shop.facade.OrderFacade;
import com.shop.global.auth.Login;
import com.shop.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;
    private final OrderFacade orderFacade;

    @PostMapping
    public CommonResponse<OrderResponse> order(@Login String email,
                                               @RequestBody @Validated OrderCreateRequest request) {
        return orderFacade.orderWithRedisson(email, request);
    }

    @PostMapping("/{orderId}/cancel")
    public void cancel(@Login String email, @PathVariable Long orderId) {
        orderService.cancel(email, orderId);
    }

    @GetMapping("/{orderId}")
    public CommonResponse<OrderResponse> getMyOrder(@Login String email, @PathVariable Long orderId) {
        return orderService.get(email, orderId);
    }

    @GetMapping("/complex-search")
    public CommonResponse<Page<OrderResponse>> searchOrders(
            @ModelAttribute @Validated OrderSearchRequest request,
            Pageable pageable) {
        return orderService.searchOrders(request, pageable);
    }

    @GetMapping("/stats")
    public CommonResponse<Page<OrderStatisticsResponse>> getOrderStats(
            @RequestParam(required = false) Long minAmount,
            Pageable pageable) {
        return orderService.getImprovedOrderStatistics(minAmount, pageable);
    }
}
