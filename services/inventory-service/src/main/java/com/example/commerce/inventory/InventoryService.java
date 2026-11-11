package com.example.commerce.inventory;

import com.example.commerce.inventory.reservation.ReservationStatus;
import com.example.commerce.inventory.reservation.StockReservation;
import com.example.commerce.inventory.reservation.StockReservationMapper;
import com.example.commerce.inventory.stock.Stock;
import com.example.commerce.inventory.stock.StockMapper;
import com.example.commerce.platform.common.error.BadRequestException;
import com.example.commerce.platform.common.error.ConflictException;
import com.example.commerce.platform.common.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static com.example.commerce.inventory.InventoryDtos.CreateStockRequest;
import static com.example.commerce.inventory.InventoryDtos.ReleaseStockRequest;
import static com.example.commerce.inventory.InventoryDtos.ReservationResponse;
import static com.example.commerce.inventory.InventoryDtos.ReserveStockRequest;
import static com.example.commerce.inventory.InventoryDtos.StockResponse;

@Service
public class InventoryService {

    private static final Duration RESERVATION_TTL = Duration.ofMinutes(30);

    private final StockMapper stockMapper;
    private final StockReservationMapper reservationMapper;

    public InventoryService(StockMapper stockMapper,
                            StockReservationMapper reservationMapper) {
        this.stockMapper = stockMapper;
        this.reservationMapper = reservationMapper;
    }

    @Transactional
    public StockResponse createStock(CreateStockRequest request) {
        if (stockMapper.countById(request.productId()) > 0) {
            throw new ConflictException("Stock already exists for product: " + request.productId());
        }
        Stock stock = new Stock(request.productId(), request.availableStock(), Instant.now());
        stockMapper.insert(stock);
        return stockResponse(stock);
    }

    @Transactional(readOnly = true)
    public StockResponse getStock(String productId) {
        return stockMapper.findById(productId)
                .map(this::stockResponse)
                .orElseThrow(() -> new NotFoundException("Stock not found for product: " + productId));
    }

    @Transactional
    public ReservationResponse reserve(ReserveStockRequest request) {
        StockReservation existing = reservationMapper
                .findByOrderIdAndProductId(request.orderId(), request.productId())
                .orElse(null);
        if (existing != null) {
            if (existing.getStatus() == ReservationStatus.RELEASED) {
                throw new ConflictException("The reservation for this order has already been released");
            }
            return reservationResponse(existing);
        }

        int updated = stockMapper.reserve(request.productId(), request.quantity(), Instant.now());
        if (updated == 0) {
            if (stockMapper.countById(request.productId()) == 0) {
                throw new NotFoundException("Stock not found for product: " + request.productId());
            }
            throw new ConflictException("Insufficient stock for product: " + request.productId());
        }

        Instant now = Instant.now();
        StockReservation reservation = new StockReservation(
                UUID.randomUUID().toString(),
                request.orderId(),
                request.productId(),
                request.quantity(),
                now.plus(RESERVATION_TTL),
                now
        );
        reservationMapper.insert(reservation);
        return reservationResponse(reservation);
    }

    @Transactional
    public ReservationResponse release(ReleaseStockRequest request) {
        StockReservation reservation = reservationMapper
                .findByOrderIdAndProductIdForUpdate(request.orderId(), request.productId())
                .orElseThrow(() -> new NotFoundException("Reservation not found"));
        if (reservation.getStatus() == ReservationStatus.RELEASED) {
            return reservationResponse(reservation);
        }

        int updated = stockMapper.release(
                reservation.getProductId(), reservation.getQuantity(), Instant.now());
        if (updated == 0) {
            throw new BadRequestException("Reserved stock cannot be released");
        }
        reservation.release(Instant.now());
        reservationMapper.updateRelease(reservation.getId(), reservation.getReleasedAt());
        return reservationResponse(reservation);
    }

    private StockResponse stockResponse(Stock stock) {
        return new StockResponse(
                stock.getProductId(),
                stock.getAvailableStock(),
                stock.getReservedStock(),
                stock.getUpdatedAt()
        );
    }

    private ReservationResponse reservationResponse(StockReservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getOrderId(),
                reservation.getProductId(),
                reservation.getQuantity(),
                reservation.getStatus().name(),
                reservation.getExpiresAt()
        );
    }
}
