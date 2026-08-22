package com.yuvraj.productservice.service;

import com.yuvraj.productservice.dto.CreateProductRequest;
import com.yuvraj.productservice.dto.ProductResponse;
import com.yuvraj.productservice.entity.ProductEntity;
import com.yuvraj.productservice.exception.ProductNotFoundException;
import com.yuvraj.productservice.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Creates a product owned by the calling user.
     *
     * @param creatorUserId the userId resolved by the gateway from the JWT
     */
    @Transactional
    public ProductResponse create(CreateProductRequest request, Long creatorUserId) {
        ProductEntity saved = productRepository.save(new ProductEntity(
                request.name(), request.description(), request.price(), creatorUserId));
        return ProductResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return productRepository.findById(id)
                .map(ProductResponse::from)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    /**
     * Returns a page of products. Backed by {@code GET /products?page=&size=}.
     */
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAll(Pageable pageable) {
        return productRepository.findAll(pageable).map(ProductResponse::from);
    }
}
