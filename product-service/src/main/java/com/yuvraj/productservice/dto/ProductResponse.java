package com.yuvraj.productservice.dto;

import com.yuvraj.productservice.entity.ProductEntity;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Long createdBy,
        Instant createdAt
) {
    public static ProductResponse from(ProductEntity product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCreatedBy(),
                product.getCreatedAt());
    }
}
