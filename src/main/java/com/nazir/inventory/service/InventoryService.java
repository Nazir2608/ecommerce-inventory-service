package com.nazir.inventory.service;

import com.nazir.inventory.dto.ProductRequest;
import com.nazir.inventory.dto.ProductResponse;
import com.nazir.inventory.dto.ReservationRequest;
import com.nazir.inventory.dto.ReservationResponse;

public interface InventoryService {
    ProductResponse createProduct(ProductRequest request);
    ProductResponse getProduct(Long id);
    ReservationResponse reserveInventory(ReservationRequest request);
}
