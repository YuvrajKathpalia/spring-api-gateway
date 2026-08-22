package com.yuvraj.productservice.repository;

import com.yuvraj.productservice.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * {@link JpaRepository} already provides {@code findAll(Pageable)}, which the
 * service uses to support {@code GET /products?page=&size=}.
 */
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
}
