package com.shop.repository;

import com.shop.domain.payment.Payment;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("SELECT COUNT(p) FROM Payment p WHERE DATE(p.requestedAt) = :date")
    long countByRequestedAt(@Param("date") LocalDate date);

    Optional<Payment> findPaymentByPaymentKey(String paymentKey);
}
