package com.example.commerce.catalog.product;

import jakarta.validation.Valid;
import com.example.commerce.platform.common.web.PageResult;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static com.example.commerce.catalog.product.ProductDtos.CreateProductRequest;
import static com.example.commerce.catalog.product.ProductDtos.ProductResponse;
import static com.example.commerce.catalog.product.ProductDtos.UpdateProductImageRequest;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    PageResult<ProductResponse> list(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return productService.list(Math.max(page, 0), safeSize);
    }

    @GetMapping("/{id}")
    ProductResponse get(@PathVariable String id) {
        return productService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    ProductResponse create(@Valid @RequestBody CreateProductRequest request) {
        return productService.create(request);
    }

    @PatchMapping("/{id}/image")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    ProductResponse updateImage(@PathVariable String id,
                                @Valid @RequestBody UpdateProductImageRequest request) {
        return productService.updateImage(id, request);
    }
}
