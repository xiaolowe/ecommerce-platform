package com.example.commerce.payment.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {

    Optional<Payment> findByPaymentNo(String paymentNo);

    Optional<Payment> findByOrderId(String orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select payment from Payment payment where payment.paymentNo = :paymentNo")
    Optional<Payment> findByPaymentNoForUpdate(@Param("paymentNo") String paymentNo);

    List<Payment> findTop100ByStatusAndOrderSyncedFalseOrderByPaidAtAsc(PaymentStatus status);

    List<Payment> findTop100ByStatusAndExpiresAtBeforeOrderByExpiresAtAsc(
            PaymentStatus status, Instant cutoff);

    @Query("""
            select payment from Payment payment
            where (:status is null or payment.status = :status)
              and (:keyword is null
                   or lower(payment.paymentNo) like lower(concat('%', :keyword, '%'))
                   or lower(payment.orderId) like lower(concat('%', :keyword, '%'))
                   or lower(payment.customerId) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(payment.providerTradeNo, '')) like lower(concat('%', :keyword, '%')))
            """)
    Page<Payment> searchForAdmin(@Param("status") PaymentStatus status,
                                 @Param("keyword") String keyword,
                                 Pageable pageable);
}
