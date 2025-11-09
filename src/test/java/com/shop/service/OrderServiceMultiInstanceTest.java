package com.shop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.shop.ShopApplication;
import com.shop.domain.product.Product;
import com.shop.dto.request.OrderCreateRequest;
import com.shop.facade.OrderFacade;
import com.shop.repository.order.OrderRepository;
import com.shop.repository.product.ProductRepository;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Slf4j
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrderServiceMultiInstanceTest {

    //H2 공유 (두 컨텍스트가 동일 URL 사용)
    static final String H2_URL = "jdbc:h2:mem:test;NON_KEYWORDS=USER";
    static final String H2_USER = "sa";
    static final String H2_PASS = "";
    private static final int THREAD_COUNT = 1000;
    private static final long PRODUCT_ID = 1L;

    //Redis (분산락)
    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
                    .withExposedPorts(6379);

    //서로 다른 Spring 컨텍스트
    static ConfigurableApplicationContext appA;
    static ConfigurableApplicationContext appB;

    //각 인스턴스의 서비스 빈
    static OrderService orderServiceA;
    static OrderService orderServiceB;
    static OrderFacade orderFacadeA;
    static OrderFacade orderFacadeB;
    static OrderRepository orderRepositoryA;
    static ProductRepository productRepositoryA;

    @BeforeAll
    static void bootTwoInstances() throws Exception {
        assertTrue(redis.isRunning());

        String redisHost = redis.getHost();
        Integer redisPort = redis.getMappedPort(6379);

        //App A: 스키마 생성 전담
        appA = new SpringApplicationBuilder(ShopApplication.class)
                .web(WebApplicationType.NONE)
                .properties(
                        "spring.data.redis.host=" + redisHost,
                        "spring.data.redis.port=" + redisPort,

                        "spring.datasource.url=" + H2_URL,
                        "spring.datasource.username=" + H2_USER,
                        "spring.datasource.password=" + H2_PASS,
                        "spring.datasource.driver-class-name=org.h2.Driver",

                        "spring.jpa.hibernate.ddl-auto=create",
                        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",

                        "instance.id=A",
                        "logging.level.root=INFO"
                )
                .run();

        //App B: 스키마 건드리지 않음
        appB = new SpringApplicationBuilder(ShopApplication.class)
                .web(WebApplicationType.NONE)
                .properties(
                        "spring.data.redis.host=" + redisHost,
                        "spring.data.redis.port=" + redisPort,

                        "spring.datasource.url=" + H2_URL,
                        "spring.datasource.username=" + H2_USER,
                        "spring.datasource.password=" + H2_PASS,
                        "spring.datasource.driver-class-name=org.h2.Driver",

                        "spring.jpa.hibernate.ddl-auto=none",
                        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",

                        "instance.id=B",
                        "logging.level.root=INFO"
                )
                .run();

        //서비스 빈 참조
        orderServiceA = appA.getBean(OrderService.class);
        orderServiceB = appB.getBean(OrderService.class);
        orderFacadeA = appA.getBean(OrderFacade.class);
        orderFacadeB = appB.getBean(OrderFacade.class);
        orderRepositoryA = appA.getBean(OrderRepository.class);
        productRepositoryA = appA.getBean(ProductRepository.class);

        //초기 데이터 주입(공유 H2에 직접)
        try (Connection c = DriverManager.getConnection(H2_URL, H2_USER, H2_PASS)) {
            c.prepareStatement("DELETE FROM orders").executeUpdate();
            c.prepareStatement("DELETE FROM member").executeUpdate();
            c.prepareStatement("DELETE FROM product").executeUpdate();

            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO product(product_id, name, price, stock_quantity) VALUES(?, ?, ?, ?)")) {
                ps.setLong(1, PRODUCT_ID);
                ps.setString(2, "테스트상품");
                ps.setLong(3, 1000);
                ps.setLong(4, THREAD_COUNT);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO member(email, name) VALUES(?, ?)")) {
                for (int i = 0; i < THREAD_COUNT; i++) {
                    ps.setString(1, "user" + i + "@example.com");
                    ps.setString(2, "사용자" + i);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
    }

    @AfterAll
    static void shutdownApps() {
        if (appA != null) {
            appA.close();
        }
        if (appB != null) {
            appB.close();
        }
    }

    @Test
    @Order(0)
    @DisplayName("멀티 인스턴스 환경에서 분산락 미적용 시 동시성 문제 발생")
    void concurrentOrdersWithoutLock_causesRaceCondition() throws Exception {
        //Java 17: 캐시드 풀(필요 시 1000개 스레드까지 늘어남). 메모리 여유 없으면 fixed(THREADS)도 가능.
        ExecutorService pool = Executors.newCachedThreadPool();

        CountDownLatch ready = new CountDownLatch(THREAD_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    ready.countDown();
                    start.await();

                    OrderCreateRequest req = OrderCreateRequest.builder()
                            .productId(PRODUCT_ID)
                            .quantity(1)
                            .build();

                    //절반은 A, 절반은 B 인스턴스 호출 (락 없음)
                    if ((idx & 1) == 0) {
                        orderServiceA.order("user" + idx + "@example.com", req);
                    } else {
                        orderServiceB.order("user" + idx + "@example.com", req);
                    }
                } catch (Exception ignore) {
                } finally {
                    done.countDown();
                }
            });
        }

        assertTrue(ready.await(60, TimeUnit.SECONDS), "작업 시작 대기 초과");
        start.countDown();
        assertTrue(done.await(180, TimeUnit.SECONDS), "완료 대기 초과");

        pool.shutdown();
        assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS), "스레드풀 종료 대기 초과");

        long orders = orderRepositoryA.count();
        long stock;
        try (Connection c = DriverManager.getConnection(H2_URL, H2_USER, H2_PASS)) {
            var rs = c.createStatement().executeQuery(
                    "SELECT stock_quantity FROM product WHERE product_id=" + PRODUCT_ID);
            rs.next();
            stock = rs.getLong(1);
        }

        assertEquals(THREAD_COUNT, orders, "주문 엔티티 수는 1000건");
        assertNotEquals(0L, stock, "경쟁으로 재고가 0까지 내려가지 않음(동시성 문제 재현)");
    }

    @Test
    @Order(1)
    @DisplayName("멀티 인스턴스 환경에서 1,000명 동시 주문 시 분산락으로 재고 정합성 보장")
    void concurrentOrdersWithDistributedLock_ensuresConsistency() throws Exception {
        //given
        try (Connection c = DriverManager.getConnection(H2_URL, H2_USER, H2_PASS)) {
            c.prepareStatement("DELETE FROM orders").executeUpdate();
            c.prepareStatement("UPDATE product SET stock_quantity=" + THREAD_COUNT + " WHERE product_id=" + PRODUCT_ID)
                    .executeUpdate();
        }

        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        //when
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int idx = i;
            executorService.submit(() -> {
                try {
                    OrderCreateRequest request = OrderCreateRequest.builder()
                            .productId(PRODUCT_ID)
                            .quantity(1)
                            .build();

                    //번갈아 가면서 주문 요청
                    String email = "user" + idx + "@example.com";
                    if ((idx & 1) == 0) {
                        orderFacadeA.orderWithRedisson(email, request);
                    } else {
                        orderFacadeB.orderWithRedisson(email, request);
                    }
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
        long orderCount = orderRepositoryA.count();
        Product product = productRepositoryA.findById(PRODUCT_ID).get();
        int stockQuantity = product.getStockQuantity();

        assertEquals(THREAD_COUNT, orderCount, "모든 주문이 정상 반영");
        assertEquals(0, stockQuantity, "모든 재고 소진");
    }
}
