package com.example.commerce.payment.payment;

import com.example.commerce.payment.client.OrderClient;
import com.example.commerce.payment.provider.PaymentProviderRegistry;
import com.example.commerce.platform.common.error.BadRequestException;
import com.example.commerce.platform.common.error.ConflictException;
import com.example.commerce.platform.common.error.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static com.example.commerce.payment.payment.PaymentDtos.CreatePaymentRequest;
import static com.example.commerce.payment.payment.PaymentDtos.PaymentResponse;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentProviderRegistry providerRegistry;
    private final OrderClient orderClient;
    private final TransactionTemplate transactionTemplate;
    private final long paymentTtlMinutes;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentProviderRegistry providerRegistry,
                          OrderClient orderClient,
                          TransactionTemplate transactionTemplate,
                          @Value("${payment.ttl-minutes:15}") long paymentTtlMinutes) {
        this.paymentRepository = paymentRepository;
        this.providerRegistry = providerRegistry;
        this.orderClient = orderClient;
        this.transactionTemplate = transactionTemplate;
        this.paymentTtlMinutes = Math.max(1, paymentTtlMinutes);
    }

    public PaymentResponse create(String customerId, String accessToken, CreatePaymentRequest request) {
        Payment existing = paymentRepository.findByOrderId(request.orderId()).orElse(null);
        if (existing != null) {
            assertOwner(existing, customerId);
            return toResponse(existing);
        }

        String channel = providerRegistry.require(request.channel()).channel();
        OrderClient.OrderSnapshot order = orderClient.getOrder(request.orderId(), accessToken);
        if (!customerId.equals(order.customerId())) {
            throw new BadRequestException("Order does not belong to the current customer");
        }
        if (!"PENDING_PAYMENT".equals(order.status())) {
            throw new ConflictException("Order is not waiting for payment");
        }

        Instant now = Instant.now();
        Payment payment = new Payment(
                UUID.randomUUID().toString(),
                nextPaymentNo(),
                order.id(),
                customerId,
                order.totalCents(),
                order.currency(),
                channel,
                now.plusSeconds(paymentTtlMinutes * 60),
                now
        );
        try {
            return toResponse(paymentRepository.saveAndFlush(payment));
        } catch (DataIntegrityViolationException exception) {
            Payment concurrent = paymentRepository.findByOrderId(request.orderId())
                    .orElseThrow(() -> exception);
            assertOwner(concurrent, customerId);
            return toResponse(concurrent);
        }
    }

    @Transactional(readOnly = true)
    public PaymentResponse get(String customerId, String paymentNo) {
        Payment payment = paymentRepository.findByPaymentNo(paymentNo)
                .orElseThrow(() -> new NotFoundException("Payment not found: " + paymentNo));
        assertOwner(payment, customerId);
        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> listForAdmin(PaymentStatus status, String keyword, Pageable pageable) {
        return paymentRepository.searchForAdmin(status, normalizeKeyword(keyword), pageable)
                .map(this::toResponse);
    }

    public PaymentResponse mockPay(String customerId, String paymentNo) {
        Payment paid = transactionTemplate.execute(status -> markSucceeded(customerId, paymentNo));
        if (paid == null) {
            throw new IllegalStateException("Payment transaction did not return a payment");
        }
        if (!"MOCK".equals(paid.getChannel())) {
            throw new BadRequestException("Mock payment is not supported for this channel");
        }
        try {
            orderClient.markOrderPaid(paid.getOrderId(), paid.getAmountCents(),
                    paid.getCurrency(), paid.getPaymentNo());
            transactionTemplate.executeWithoutResult(status -> markOrderSynced(paid.getId()));
        } catch (RuntimeException exception) {
            log.warn("Payment {} succeeded but order {} sync is pending",
                    paid.getPaymentNo(), paid.getOrderId(), exception);
        }
        return get(customerId, paymentNo);
    }

    private Payment markSucceeded(String customerId, String paymentNo) {
        Payment payment = paymentRepository.findByPaymentNoForUpdate(paymentNo)
                .orElseThrow(() -> new NotFoundException("Payment not found: " + paymentNo));
        assertOwner(payment, customerId);
        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            return payment;
        }
        if (payment.getExpiresAt().isBefore(Instant.now())) {
            throw new ConflictException("Payment has expired");
        }

        payment.succeed("MOCK-" + UUID.randomUUID().toString().replace("-", ""), Instant.now());
        return paymentRepository.saveAndFlush(payment);
    }

    private void markOrderSynced(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment not found: " + paymentId));
        payment.markOrderSynced();
        paymentRepository.save(payment);
    }

    @Scheduled(fixedDelayString = "${jobs.payment-reconciliation.delay-ms:30000}")
    public void reconcileSuccessfulPayments() {
        List<Payment> pending = paymentRepository
                .findTop100ByStatusAndOrderSyncedFalseOrderByPaidAtAsc(PaymentStatus.SUCCEEDED);
        for (Payment payment : pending) {
            try {
                orderClient.markOrderPaid(payment.getOrderId(), payment.getAmountCents(),
                        payment.getCurrency(), payment.getPaymentNo());
                transactionTemplate.executeWithoutResult(status -> markOrderSynced(payment.getId()));
            } catch (RuntimeException exception) {
                log.warn("Order sync still pending for payment {}", payment.getPaymentNo(), exception);
            }
        }
    }

    @Scheduled(fixedDelayString = "${jobs.payment-expiration.delay-ms:30000}")
    @Transactional
    public void closeExpiredPayments() {
        paymentRepository.findTop100ByStatusAndExpiresAtBeforeOrderByExpiresAtAsc(
                        PaymentStatus.REQUIRES_ACTION, Instant.now())
                .forEach(payment -> payment.close(Instant.now()));
    }

    private void assertOwner(Payment payment, String customerId) {
        if (!payment.getCustomerId().equals(customerId)) {
            throw new NotFoundException("Payment not found");
        }
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    private String nextPaymentNo() {
        return "PAY" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .format(java.time.LocalDateTime.now())
                + ThreadLocalRandom.current().nextInt(100000, 999999);
    }

    private PaymentResponse toResponse(Payment payment) {
        String confirmPath = payment.getStatus() == PaymentStatus.REQUIRES_ACTION
                ? providerRegistry.require(payment.getChannel()).initiate(payment).confirmPath()
                : null;
        return new PaymentResponse(
                payment.getId(),
                payment.getPaymentNo(),
                payment.getOrderId(),
                payment.getAmountCents(),
                payment.getCurrency(),
                payment.getChannel(),
                payment.getStatus().name(),
                payment.getProviderTradeNo(),
                payment.isOrderSynced(),
                confirmPath,
                payment.getExpiresAt(),
                payment.getPaidAt(),
                payment.getCreatedAt()
        );
    }
}
