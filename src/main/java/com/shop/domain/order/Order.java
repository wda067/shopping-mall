package com.shop.domain.order;

import static com.shop.domain.order.OrderStatus.CANCELED;
import static com.shop.domain.order.OrderStatus.COMPLETED;
import static com.shop.domain.order.OrderStatus.PAYMENT_PENDING;

import com.shop.domain.member.Member;
import com.shop.domain.payment.Payment;
import com.shop.global.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private String orderNumber;

    private String orderName;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private final List<OrderProduct> orderProducts = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    private String email;

    @OneToOne(mappedBy = "order")
    private Payment payment;

    @Setter
    private String paymentKey;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private LocalDateTime orderDate;

    private int totalAmount;

    private int totalQuantity;

    public Order(Member member, List<OrderProduct> orderProducts) {
        this.member = member;
        this.email = member.getEmail();
        orderProducts.forEach(this::addOrderProduct);
        this.orderNumber = generateOrderNumber();
        this.orderName = generateOrderName();
        this.status = OrderStatus.NEW;
        this.orderDate = LocalDateTime.now();
        this.totalAmount = orderProducts.stream()
                .mapToInt(OrderProduct::getTotalPrice)
                .sum();
        this.totalQuantity = orderProducts.size();
    }

    private String generateOrderNumber() {
        return "ORD-" + LocalDate.now() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    public void addOrderProduct(OrderProduct orderProduct) {
        orderProducts.add(orderProduct);
        orderProduct.setOrder(this);
    }

    public void cancel() {
        this.status = OrderStatus.CANCELED;
        orderProducts.forEach(OrderProduct::cancel);
    }

    public void processPayment() {
        this.status = PAYMENT_PENDING;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
        this.status = COMPLETED;
        this.paymentKey = payment.getPaymentKey();
    }

    //public String getOrderName() {
    //    if (orderProducts.size() == 1) {
    //        return orderProducts.get(0).getProduct().getName();
    //    } else {
    //        return orderProducts.get(0).getProduct().getName()
    //                + "외 " + orderProducts.size() + "건";
    //    }
    //}

    private String generateOrderName() {
        if (orderProducts.size() == 1) {
            return orderProducts.get(0).getProduct().getName();
        } else {
            return orderProducts.get(0).getProduct().getName()
                    + "외 " + orderProducts.size() + "건";
        }
    }
}
