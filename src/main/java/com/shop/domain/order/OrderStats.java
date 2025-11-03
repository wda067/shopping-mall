package com.shop.domain.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStats {

    @Id
    @Column(name = "member_id")
    private Long id;

    private String email;
    private int orderCount;
    private long totalAmount;
    private double averageAmount;
    private LocalDateTime lastOrderDate;
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void updateOrderStats() {
        this.updatedAt = LocalDateTime.now();
    }
}
