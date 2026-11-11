package com.example.commerce.catalog.product;

import com.example.commerce.platform.common.error.ConflictException;
import com.example.commerce.platform.common.error.NotFoundException;
import com.example.commerce.platform.common.web.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.example.commerce.catalog.product.ProductDtos.CreateProductRequest;
import static com.example.commerce.catalog.product.ProductDtos.ProductResponse;
import static com.example.commerce.catalog.product.ProductDtos.UpdateProductImageRequest;

@Service
public class ProductService {

    private final ProductMapper productMapper;

    public ProductService(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    @Transactional(readOnly = true)
    public PageResult<ProductResponse> list(int page, int size) {
        List<ProductResponse> content = productMapper.findPage(page * size, size).stream()
                .map(this::toResponse)
                .toList();
        return PageResult.of(content, page, size, productMapper.count());
    }

    @Transactional(readOnly = true)
    public ProductResponse get(String id) {
        return productMapper.findActiveById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Product not found: " + id));
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        String sku = request.sku().trim().toUpperCase(Locale.ROOT);
        if (productMapper.countBySku(sku) > 0) {
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
        productMapper.insert(product);
        return toResponse(product);
    }

    @Transactional
    public ProductResponse updateImage(String id, UpdateProductImageRequest request) {
        Product product = productMapper.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found: " + id));
        product.updateImage(normalizeImageUrl(request.imageUrl()));
        productMapper.updateImage(id, product.getImageUrl(), product.getUpdatedAt());
        return toResponse(product);
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
