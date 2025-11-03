package com.shop.service;

import com.shop.domain.member.Member;
import com.shop.domain.order.Order;
import com.shop.domain.order.OrderProduct;
import com.shop.domain.order.OrderStats;
import com.shop.domain.product.Product;
import com.shop.dto.request.OrderCreateRequest;
import com.shop.dto.request.OrderSearchRequest;
import com.shop.dto.response.CommonResponse;
import com.shop.dto.response.OrderResponse;
import com.shop.dto.response.OrderStatisticsResponse;
import com.shop.exception.MemberNotFound;
import com.shop.exception.OrderError;
import com.shop.exception.OrderLockFailed;
import com.shop.exception.OrderMemberMismatch;
import com.shop.exception.OrderNotFound;
import com.shop.exception.ProductNotFound;
import com.shop.repository.member.MemberRepository;
import com.shop.repository.order.OrderRepository;
import com.shop.repository.order.OrderStatsRepository;
import com.shop.repository.product.ProductRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private static final Logger orderLogger = LoggerFactory.getLogger("OrderLogger");
    private static final String LOCK_KEY = "order_lock";

    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final RedissonClient redissonClient;
    private final OrderStatsRepository orderStatsRepository;

    public void order(String email, OrderCreateRequest request) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(MemberNotFound::new);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(ProductNotFound::new);
        product.removeStock(request.getQuantity());

        OrderProduct orderProduct = new OrderProduct(product, request.getQuantity());
        List<OrderProduct> orderProducts = new ArrayList<>();
        orderProducts.add(orderProduct);

        Order order = new Order(member, orderProducts);
        orderRepository.save(order);
        orderLogger.info("주문 성공");
    }

    public synchronized void orderWithSynchronized(String email, OrderCreateRequest request) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(MemberNotFound::new);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(ProductNotFound::new);
        product.removeStock(request.getQuantity());

        OrderProduct orderProduct = new OrderProduct(product, request.getQuantity());
        List<OrderProduct> orderProducts = new ArrayList<>();
        orderProducts.add(orderProduct);

        Order order = new Order(member, orderProducts);
        orderRepository.save(order);
    }

    public synchronized void orderWithRedisson(String email, OrderCreateRequest request) {
        RLock lock = redissonClient.getLock(LOCK_KEY);

        try {
            if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                try {

                    Member member = memberRepository.findByEmail(email)
                            .orElseThrow(MemberNotFound::new);
                    Product product = productRepository.findById(request.getProductId())
                            .orElseThrow(ProductNotFound::new);
                    product.removeStock(request.getQuantity());

                    OrderProduct orderProduct = new OrderProduct(product, request.getQuantity());
                    List<OrderProduct> orderProducts = new ArrayList<>();
                    orderProducts.add(orderProduct);

                    Order order = new Order(member, orderProducts);
                    orderRepository.save(order);

                } finally {
                    lock.unlock();
                }
            } else {
                throw new OrderLockFailed();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OrderError();
        }
    }

    public void cancel(String email, Long orderId) {
        Order order = getOrderForMember(email, orderId);
        order.cancel();
    }

    @Transactional(readOnly = true)
    public CommonResponse<OrderResponse> get(String email, Long orderId) {
        Order order = getOrderForMember(email, orderId);
        OrderResponse orderResponse = toOrderResponse(order);

        return CommonResponse.success(orderResponse);
    }

    @Transactional(readOnly = true)
    public CommonResponse<Page<OrderResponse>> searchOrders(OrderSearchRequest request,
                                                            Pageable pageable) {
        LocalDateTime startDate = request.getStartDate();
        LocalDateTime endDate = request.getEndDate();
        Integer year = request.getYear();

        //특정 연도 기준
        if (year != null) {
            startDate = LocalDate.of(year, 1, 1).atStartOfDay();
            endDate = LocalDate.of(year + 1, 1, 1).atStartOfDay();
        }

        //최근 6개월 기준
        if (startDate == null && endDate == null) {
            endDate = LocalDateTime.now();
            startDate = endDate.minusMonths(6);
        }

        if (startDate == null) {
            startDate = endDate.minusMonths(6);
        }
        if (endDate == null) {
            endDate = startDate.plusMonths(6);
        }

        Page<OrderResponse> page = orderRepository.findOrdersByComplexCondition(startDate, endDate,
                        request.getStatus(), request.getMinAmount(), pageable)
                .map(this::toOrderResponse);
        return CommonResponse.success(page);
    }

    public CommonResponse<Page<OrderStatisticsResponse>> getOrderStatistics(Long minAmount, Pageable pageable) {
        Page<OrderStatisticsResponse> page = orderRepository.getOrdersStatistics(minAmount, pageable)
                .map(projection -> OrderStatisticsResponse.builder()
                        .email(projection.getEmail())
                        .totalOrders(projection.getTotalOrders())
                        .totalAmount(projection.getTotalAmount())
                        .averageAmount(projection.getAverageAmount())
                        .lastOrderDate(projection.getLastOrderDate())
                        .build());
        return CommonResponse.success(page);
    }

    public CommonResponse<Page<OrderStatisticsResponse>> getImprovedOrderStatistics(Long minAmount, Pageable pageable) {
        if (minAmount == null) {
            minAmount = 0L;
        }

        Page<OrderStatisticsResponse> page = orderStatsRepository.findByTotalAmountGreaterThanEqual(
                        minAmount, pageable)
                .map(this::toOrderStatisticsResponse);
        return CommonResponse.success(page);
    }

    //private OrderResponse toOrderResponse(Order order) {
    //    return OrderResponse.builder()
    //            .email(order.getMember().getEmail())
    //            .orderNumber(order.getOrderNumber())
    //            .orderName(order.getOrderName())
    //            .status(order.getStatus())
    //            .orderDate(order.getOrderDate())
    //            .totalAmount((long) order.getTotalAmount())
    //            .totalQuantity(order.getOrderProducts().size())
    //            .build();
    //}

    private OrderResponse toOrderResponse(Order order) {
        return OrderResponse.builder()
                .email(order.getEmail())
                .orderNumber(order.getEmail())
                .orderName(order.getOrderName())
                .status(order.getStatus())
                .orderDate(order.getOrderDate())
                .totalAmount((long) order.getTotalAmount())
                .totalQuantity(order.getTotalQuantity())
                .build();
    }

    private OrderStatisticsResponse toOrderStatisticsResponse(OrderStats stats) {
        return OrderStatisticsResponse.builder()
                .email(stats.getEmail())
                .totalOrders((long) stats.getOrderCount())
                .totalAmount(stats.getTotalAmount())
                .averageAmount(stats.getAverageAmount())
                .lastOrderDate(stats.getLastOrderDate())
                .build();
    }

    private Order getOrderForMember(String email, Long orderId) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(MemberNotFound::new);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(OrderNotFound::new);

        Member orderMember = order.getMember();
        boolean isNotOrderedMember = !member.getId().equals(orderMember.getId());
        if (isNotOrderedMember) {
            throw new OrderMemberMismatch();
        }

        return order;
    }
}
