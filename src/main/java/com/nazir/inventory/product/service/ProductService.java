package com.nazir.inventory.product.service;

import com.nazir.inventory.product.dto.ProductRequest;
import com.nazir.inventory.product.dto.ProductResponse;

public interface ProductService {
    ProductResponse createProduct(ProductRequest request);
    ProductResponse getProduct(Long id);
}