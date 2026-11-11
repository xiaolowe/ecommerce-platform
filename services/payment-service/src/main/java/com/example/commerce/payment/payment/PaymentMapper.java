package com.example.commerce.payment.payment;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Mapper
public interface PaymentMapper {

    Optional<Payment> findByPaymentNo(String paymentNo);

    Optional<Payment> findByOrderId(String orderId);

    Optional<Payment> findById(String id);

    Optional<Payment> findByPaymentNoForUpdate(String paymentNo);

    int insert(Payment payment);

    int update(Payment payment);

    int deleteAll();

    List<Payment> findAdminPage(
            @Param("status") PaymentStatus status,
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("limit") int limit);

    long countAdmin(@Param("status") PaymentStatus status, @Param("keyword") String keyword);

    List<Payment> findSuccessfulUnsynced(@Param("limit") int limit);

    List<Payment> findExpiredRequiresAction(@Param("now") Instant now, @Param("limit") int limit);
}
