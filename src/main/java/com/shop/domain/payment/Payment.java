package com.shop.domain.payment;

import static jakarta.persistence.FetchType.LAZY;
import static lombok.AccessLevel.PROTECTED;

import com.shop.domain.member.Member;
import com.shop.domain.order.Order;
import com.shop.dto.response.PaymentResponse;
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
import jakarta.persistence.OneToOne;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Payment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    private String paymentKey;

    private String orderName;

    private String method;

    private Long totalAmount;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Enumerated(EnumType.STRING)
    PaymentStatus status;

    private LocalDateTime requestedAt;

    public Payment(PaymentResponse response, Order order) {
        this.paymentKey = response.getPaymentKey();
        this.orderName = response.getOrderName();
        this.method = response.getMethod();
        this.totalAmount = response.getTotalAmount();
        setOrder(order);
        this.member = order.getMember();
        this.status = PaymentStatus.fromString(response.getStatus());
        this.requestedAt = response.getRequestedAt();
    }

    public void setOrder(Order order) {
        this.order = order;
        order.setPayment(this);
    }
}
