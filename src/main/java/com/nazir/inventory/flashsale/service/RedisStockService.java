package com.nazir.inventory.flashsale.service;

import com.nazir.inventory.exception.ResourceNotFoundException;
import com.nazir.inventory.product.entity.Product;
import com.nazir.inventory.product.repository.ProductRepository;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisStockService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ProductRepository productRepository;
    private final DefaultRedisScript<Long> stockScript;

    private static final String PRODUCT_STOCK_KEY = "product:%d:stock";

    @Transactional
    public void enableFlashSale(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (Boolean.TRUE.equals(product.getFlashSaleEnabled())) {
            log.warn("Flash sale already enabled for product: {}", productId);
            // Even if enabled, re-sync stock to be safe
            syncStockToRedis(product);
            return;
        }
        
        // 1. Sync DB stock to Redis
        syncStockToRedis(product);

        // 2. Enable flag in DB
        product.setFlashSaleEnabled(true);
        productRepository.save(product);
        log.info("Enabled flash sale for product: {}", productId);
    }

    private void syncStockToRedis(Product product) {
        int availableStock = product.getAvailableStock();
        String key = String.format(PRODUCT_STOCK_KEY, product.getId());
        // Use set to overwrite any existing value
        redisTemplate.opsForValue().set(key, String.valueOf(availableStock));
        log.info("Synced Redis stock for product {}: {}", product.getId(), availableStock);
    }

    @Transactional
    public void disableFlashSale(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (!product.getFlashSaleEnabled()) {
            log.warn("Flash sale already disabled for product: {}", productId);
            return;
        }

        // 1. Remove from Redis
        String key = String.format(PRODUCT_STOCK_KEY, productId);
        redisTemplate.delete(key);
        log.info("Removed Redis stock key for product: {}", productId);

        // 2. Disable flag in DB
        product.setFlashSaleEnabled(false);
        productRepository.save(product);
        log.info("Disabled flash sale for product: {}", productId);
    }

    public boolean decrementStock(Long productId, int quantity) {
        String key = String.format(PRODUCT_STOCK_KEY, productId);
        // Execute Lua script
        Long result;
        try {
            result = redisTemplate.execute(stockScript, Collections.singletonList(key), String.valueOf(quantity));
        } catch (Exception e) {
            log.error("Error executing Redis script for product {}: {}", productId, e.getMessage());
            return false;
        }
        
        log.info("Redis decrement result for product {}: {}", productId, result);

        if (result != null && result >= 0) {
            return true;
        } else if (result == -1) {
             log.warn("Redis stock insufficient for product {}. Stock is likely 0 or less than requested quantity.", productId);
             return false;
        } else if (result == -2) {
             log.warn("Redis key missing for product {}. Flash sale might not be enabled.", productId);
             return false;
        } else {
             return false;
        }
    }

    public void restoreStock(Long productId, int quantity) {
        String key = String.format(PRODUCT_STOCK_KEY, productId);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            redisTemplate.opsForValue().increment(key, quantity);
            log.info("Restored Redis stock for product {}: +{}", productId, quantity);
        } else {
            log.warn("Skipping Redis restore for product {}: Key not found (Flash Sale disabled?)", productId);
        }
    }
}