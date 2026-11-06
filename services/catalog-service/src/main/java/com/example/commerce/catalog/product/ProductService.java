package com.example.commerce.catalog.product;

import com.example.commerce.platform.common.error.ConflictException;
import com.example.commerce.platform.common.error.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import static com.example.commerce.catalog.product.ProductDtos.CreateProductRequest;
import static com.example.commerce.catalog.product.ProductDtos.ProductResponse;
import static com.example.commerce.catalog.product.ProductDtos.UpdateProductImageRequest;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> list(Pageable pageable) {
        return productRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse get(String id) {
        return productRepository.findByIdAndStatus(id, ProductStatus.ACTIVE)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Product not found: " + id));
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        String sku = request.sku().trim().toUpperCase(Locale.ROOT);
        if (productRepository.existsBySkuIgnoreCase(sku)) {
            throw new ConflictException("SKU already exists: " + sku);
        }

        Instant now = Instant.now();
        Product product = new Product(
                UUID.randomUUID().toString(),
                sku,
                request.name().trim(),
                request.description(),
                request.priceCents(),
                request.currency().toUpperCase(Locale.ROOT),
                normalizeImageUrl(request.imageUrl()),
                ProductStatus.ACTIVE,
                now
        );
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateImage(String id, UpdateProductImageRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found: " + id));
        product.updateImage(normalizeImageUrl(request.imageUrl()));
        return toResponse(productRepository.saveAndFlush(product));
    }

    private String normalizeImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        return imageUrl.trim();
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPriceCents(),
                product.getCurrency(),
                product.getImageUrl(),
                product.getStatus().name(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
