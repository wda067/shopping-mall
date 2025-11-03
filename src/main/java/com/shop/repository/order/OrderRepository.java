package com.shop.repository.order;

import com.shop.domain.order.Order;
import com.shop.domain.order.OrderStatus;
import com.shop.repository.order.dto.OrderStatisticsProjection;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT o "
            + "FROM Order o "
            + "WHERE o.orderDate >= :startDate "
            + "AND o.orderDate < :endDate "
            + "AND o.status = :status "
            + "AND o.totalAmount >= :minAmount "
            + "ORDER BY o.orderDate DESC")
    Page<Order> findOrdersByComplexCondition(LocalDateTime startDate, LocalDateTime endDate,
                                             OrderStatus status,
                                             Integer minAmount, Pageable pageable);

    @Query(value = "SELECT "
            + "m.email as email, "
            + "COUNT(*) as totalOrders, "
            + "SUM(o.total_amount) as totalAmount, "
            + "AVG(o.total_amount) as averageAmount, "
            + "MAX(o.order_date) as lastOrderDate "
            + "FROM orders o "
            + "JOIN member m "
            + "ON o.member_id = m.member_id "
            + "GROUP BY m.email "
            + "HAVING SUM(o.total_amount) >= :minAmount",
            countQuery = "SELECT COUNT(*) "
                    + "FROM ("
                    + "SELECT 1 "
                    + "FROM orders o "
                    + "JOIN member m "
                    + "ON o.member_id = m.member_id "
                    + "GROUP BY m.email "
                    + "HAVING SUM(o.total_amount) >= :minAmount) as stats",
            nativeQuery = true)
    Page<OrderStatisticsProjection> getOrdersStatistics(Long minAmount, Pageable pageable);
}
