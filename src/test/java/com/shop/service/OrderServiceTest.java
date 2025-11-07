package com.shop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.shop.domain.member.Member;
import com.shop.domain.product.Product;
import com.shop.dto.request.OrderCreateRequest;
import com.shop.exception.NotEnoughStock;
import com.shop.facade.OrderFacade;
import com.shop.repository.member.MemberRepository;
import com.shop.repository.order.OrderRepository;
import com.shop.repository.product.ProductRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Slf4j
@SpringBootTest
@Testcontainers
class OrderServiceTest {

    private static final int THREAD_COUNT = 1000;

    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
                    .withExposedPorts(6379);
    @Autowired
    private OrderFacade orderFacade;
    @Autowired
    private OrderService orderService;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderRepository orderRepository;
    private List<Member> members;
    private Product product;
    private Long productId;

    @DynamicPropertySource
    static void redisProps(DynamicPropertyRegistry r) {
        r.add("spring.data.redis.host", redis::getHost);
        r.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @BeforeEach
    void setUp() {
        members = new ArrayList<>();
        for (int i = 1; i <= THREAD_COUNT; i++) {
            Member m = Member.builder()
                    .email("user" + i + "@example.com")
                    .name("사용자")
                    .build();
            members.add(m);
        }
        memberRepository.saveAll(members);

        product = Product.builder()
                .name("테스트상품")
                .price(1000)
                .stockQuantity(THREAD_COUNT)
                .build();
        productRepository.save(product);
        productId = product.getId();

        orderRepository.deleteAll();
    }

    @Test
    @Order(0)
    void 트랜잭션이_격리되어_있어도_1000명이_동시에_주문하면_동시성_문제가_발생한다() throws Exception {
        //given
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        //when
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    OrderCreateRequest request = OrderCreateRequest.builder()
                            .productId(productId)
                            .quantity(1)
                            .build();
                    //orderFacade.orderWithRedisson(members.get(index).getEmail(), request);
                    orderService.order(members.get(index).getEmail(), request);
                } catch (Exception e) {
                    log.error("주문 실패: {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        //then
        Product updatedProduct = productRepository.findById(productId).orElseThrow();
        assertEquals(THREAD_COUNT, orderRepository.count(), "모든 스레드의 주문이 정상 반영되어야 함");
        assertNotEquals(0L, updatedProduct.getStockQuantity(), "일부 재고만 소진됨");
    }

    @Test
    @Order(1)
    void 분산락을_이용해_1000명이_동시에_주문하면_재고가_0이된다() throws Exception {
        //given
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        //when
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    OrderCreateRequest request = OrderCreateRequest.builder()
                            .productId(productId)
                            .quantity(1)
                            .build();
                    orderFacade.orderWithRedisson(members.get(index).getEmail(), request);
                } catch (Exception e) {
                    log.error("주문 실패: {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        //then
        Product updatedProduct = productRepository.findById(productId).orElseThrow();
        assertEquals(THREAD_COUNT, orderRepository.count(), "모든 스레드의 주문이 정상 반영되어야 함");
        assertEquals(0L, updatedProduct.getStockQuantity(), "모든 재고가 소진되어야 함");
    }

    @Test
    @Order(2)
    void 락_때문에_대기하다_실패하는_요청이_있어도_정합성은_유지된다() throws Exception {
        //given
        Product newProduct = productRepository.findById(productId).orElseThrow();
        newProduct.removeStock(THREAD_COUNT - 5);
        productRepository.save(newProduct);

        ExecutorService executorService = Executors.newFixedThreadPool(32);
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int index = i;
            tasks.add(() -> {
                try {
                    OrderCreateRequest request = OrderCreateRequest.builder()
                            .productId(productId)
                            .quantity(1)
                            .build();
                    orderFacade.orderWithRedisson(members.get(index).getEmail(), request);
                    return true;
                } catch (Exception e) {
                    log.error(((NotEnoughStock) e).getErrorCode().getMessage());
                    return false;
                }
            });
        }

        List<Future<Boolean>> results = executorService.invokeAll(tasks);
        executorService.shutdown();

        long success = results.stream().filter(f -> {
            try {
                return f.get();
            } catch (Exception e) {
                return false;
            }
        }).count();

        Product updatedProduct = productRepository.findById(productId).orElseThrow();
        assertEquals(5L, success, "성공 주문 수는 5건");
        assertEquals(0L, updatedProduct.getStockQuantity(), "모든 재고가 소진되어야 함");
    }
}