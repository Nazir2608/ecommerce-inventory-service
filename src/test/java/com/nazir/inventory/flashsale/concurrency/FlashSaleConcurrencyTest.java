package com.nazir.inventory.flashsale.concurrency;

import com.nazir.inventory.product.entity.Product;
import com.nazir.inventory.product.repository.ProductRepository;
import com.nazir.inventory.reservation.dto.ReservationRequest;
import com.nazir.inventory.reservation.dto.ReservationResponse;
import com.nazir.inventory.reservation.entity.Reservation;
import com.nazir.inventory.reservation.repository.ReservationRepository;
import com.nazir.inventory.reservation.service.ReservationService;
import com.nazir.inventory.flashsale.service.FlashSaleService;
import com.nazir.inventory.flashsale.service.RedisStockService;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
public class FlashSaleConcurrencyTest {

    @Autowired
    private FlashSaleService flashSaleService;

    @Autowired
    private RedisStockService redisStockService;

    @MockBean
    private ProductRepository productRepository;

    @MockBean
    private ReservationRepository reservationRepository;

    @MockBean
    private ReservationService reservationService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private Long productId = 1L;

    @BeforeEach
    void setUp() {
        // Setup a product with 10 items
        Product product = Product.builder()
                .id(productId)
                .name("Flash Sale Product")
                .totalStock(10)
                .reservedStock(0)
                .availableStock(10)
                .flashSaleEnabled(false)
                .build();
        
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        
        // Mock reservation service to simulate successful reservation
        when(reservationService.reserveInventory(any(ReservationRequest.class)))
            .thenAnswer(invocation -> {
                ReservationRequest req = invocation.getArgument(0);
                return ReservationResponse.builder()
                        .orderId(req.getOrderId())
                        .productId(req.getProductId())
                        .quantity(req.getQuantity())
                        .build();
            });

        // Enable flash sale (syncs to Redis)
        redisStockService.enableFlashSale(productId);
    }

    @AfterEach
    // Ensure clean state even if test fails
    void tearDown() {
        if (productId != null) {
            redisStockService.disableFlashSale(productId);
        }
    }

    @Test
    void testFlashSaleConcurrency() throws InterruptedException {
        int threadCount = 1000;
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    ReservationRequest request = ReservationRequest.builder()
                            .productId(productId)
                            .orderId(UUID.randomUUID().toString())
                            .quantity(1)
                            .build();
                    
                    flashSaleService.reserveFlashSale(request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Assertions
        System.out.println("Success Count: " + successCount.get());
        System.out.println("Failure Count: " + failureCount.get());

        // 1. Only 10 should succeed (initial stock)
        assertThat(successCount.get()).isEqualTo(10);
        
        // 2. 990 should fail (rejected by Redis)
        assertThat(failureCount.get()).isEqualTo(990);

        // 4. Verify Redis state
        String key = "product:" + productId + ":stock";
        String redisStock = (String) redisTemplate.opsForValue().get(key);
        assertThat(Integer.parseInt(redisStock)).isEqualTo(0);
    }
}