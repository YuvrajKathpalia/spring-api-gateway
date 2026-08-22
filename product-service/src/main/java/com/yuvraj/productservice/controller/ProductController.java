package com.yuvraj.productservice.controller;

import com.yuvraj.productservice.dto.CreateProductRequest;
import com.yuvraj.productservice.dto.ProductResponse;
import com.yuvraj.productservice.security.AuthenticatedUser;
import com.yuvraj.productservice.security.CurrentUser;
import com.yuvraj.productservice.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Product endpoints. All are reached only through the gateway, which verifies the
 * JWT and injects the caller identity — surfaced here via {@code @CurrentUser}.
 */
@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request,
                                                  @CurrentUser AuthenticatedUser user) {
        ProductResponse created = productService.create(request, user.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    /**
     * Paginated listing. Example: {@code GET /products?page=0&size=10&sort=createdAt,desc}
     */
    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getAll(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(productService.getAll(pageable));
    }
}
