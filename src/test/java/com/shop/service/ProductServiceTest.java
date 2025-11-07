package com.shop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.shop.domain.product.Product;
import com.shop.domain.product.ProductSellStatus;
import com.shop.dto.request.ProductCreate;
import com.shop.dto.request.ProductSearch;
import com.shop.dto.response.CommonResponse;
import com.shop.dto.response.ProductResponse;
import com.shop.exception.ProductAlreadyExists;
import com.shop.repository.product.ProductRepository;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;

@Slf4j
@SpringBootTest
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    @Test
    void 상품_등록에_성공한다() {
        //given
        ProductCreate productCreate = ProductCreate.builder()
                .name("테스트 상품")
                .price(10_000)
                .stockQuantity(100)
                .description("테스트 상품입니다.")
                .build();

        //when
        productService.save(productCreate);

        //then
        assertEquals(1, productRepository.count());
        Product product = productRepository.findAll().get(0);
        assertEquals("테스트 상품", product.getName());
    }

    @Test
    void 상품_1개를_조회한다() {
        //given
        Product product = Product.builder()
                .name("테스트 상품")
                .price(10_000)
                .stockQuantity(100)
                .description("테스트 상품입니다.")
                .sellStatus(ProductSellStatus.SELL)
                .build();

        productRepository.save(product);

        //when
        CommonResponse<ProductResponse> response = productService.get(product.getId());

        //then
        assertNotNull(response);
        assertEquals("테스트 상품", response.getBody().getName());
    }

    @Test
    void 상품_목록을_페이징_처리하여_조회한다() {
        //given
        List<Product> products = IntStream.range(1, 31)
                .mapToObj(i -> Product.builder()
                        .name("테스트 상품" + i)
                        .price(10_000)
                        .stockQuantity(100)
                        .description("테스트 상품" + i + " 입니다.")
                        .sellStatus(ProductSellStatus.SELL)
                        .build())
                .toList();

        productRepository.saveAll(products);

        ProductSearch productSearch = new ProductSearch();
        //생략하면 기본 설정값 사용
        productSearch.setPage(1);
        productSearch.setSize(5);

        //when
        CommonResponse<Page<ProductResponse>> response = productService.getList(productSearch);
        List<ProductResponse> productResponses = response.getBody().getContent();

        //then
        assertEquals(5, productResponses.size());
        assertEquals("테스트 상품30", productResponses.get(0).getName());
        assertEquals("테스트 상품26", productResponses.get(4).getName());
    }

    @Test
    void 상품을_동시에_등록하면_첫번째_요청만_성공한다() throws InterruptedException {
        //given
        ProductCreate request = ProductCreate.builder()
                .name("테스트 상품")
                .price(10_000)
                .stockQuantity(100)
                .description("테스트 상품입니다.")
                .build();

        int threadCount = 10;
        ExecutorService threadPool = Executors.newFixedThreadPool(threadCount);

        //when
        for (int i = 0; i < threadCount; i++) {
            threadPool.execute(() -> {
                String threadName = Thread.currentThread().getName();
                try {
                    productService.save(request);
                    log.info("Thread {} successfully saved the product.", threadName);
                } catch (ProductAlreadyExists e) {
                    log.warn("Thread {} failed: {}", threadName, e.getErrorCode().getMessage());
                }
            });
        }
        threadPool.shutdown();
        boolean finished = threadPool.awaitTermination(10, TimeUnit.SECONDS);

        //then
        assertTrue(finished);
        assertEquals(1, productRepository.count());
    }

    @Test
    void 이미_등록된_상품_이름으로_등록하면_예외를_발생시킨다() throws InterruptedException {
        //given
        ProductCreate request = ProductCreate.builder()
                .name("테스트 상품")
                .price(10_000)
                .stockQuantity(100)
                .description("테스트 상품입니다.")
                .build();

        int threadCount = 10;
        ExecutorService threadPool = Executors.newFixedThreadPool(threadCount);

        //when
        productService.save(request);

        for (int i = 0; i < threadCount; i++) {
            threadPool.execute(() -> {
                assertThrows(ProductAlreadyExists.class, () -> productService.save(request));
            });
        }

        threadPool.shutdown();
        boolean finished = threadPool.awaitTermination(10, TimeUnit.SECONDS);

        //then
        assertTrue(finished);
        assertEquals(1, productRepository.count());
    }
}