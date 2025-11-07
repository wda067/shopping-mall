package com.shop.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.shop.domain.member.Member;
import com.shop.domain.order.Order;
import com.shop.domain.order.OrderProduct;
import com.shop.domain.payment.Payment;
import com.shop.domain.product.Product;
import com.shop.dto.response.PaymentResponse;
import com.shop.event.OrderCompletedEvent;
import com.shop.repository.PaymentRepository;
import com.shop.repository.member.MemberRepository;
import com.shop.repository.order.OrderRepository;
import com.shop.repository.product.ProductRepository;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.reactive.TransactionContext;
import org.springframework.transaction.reactive.TransactionContextManager;
import reactor.core.publisher.Mono;

@SpringBootTest
@RecordApplicationEvents
class PaymentEventServiceTest {

    @Autowired
    private PaymentTxService paymentTxService;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private ApplicationEvents applicationEvents;

    @Autowired
    private MemberRepository memberRepository;   // ★ 추가
    @Autowired
    private ProductRepository productRepository; // ★ 추가

    @MockitoSpyBean
    private EmailService emailService;

    private Member createMember(String email, String name) {
        return Member.builder()
                .email(email)
                .name(name)
                .build();
    }

    private Product createProduct(String name, int price) {
        return Product.builder()
                .name(name)
                .price(price)
                .build();
    }

    private Order saveOrder(String email, String name) {
        Member savedMember = memberRepository.save(createMember(email, name));
        Product savedProduct = productRepository.save(createProduct("상품A", 1000));

        OrderProduct op1 = new OrderProduct(savedProduct, 1);
        Order order = new Order(savedMember, List.of(op1));    // 제공된 생성자 사용
        return orderRepository.save(order);
    }

    private String encodeOrderId(long orderId) {
        String encoded = Base64.getEncoder()
                .encodeToString(String.valueOf(orderId).getBytes());
        while (encoded.length() < 6) {
            encoded = "-" + encoded;
        }
        return encoded;
    }

    private PaymentResponse buildResponse(Order order) {
        return new PaymentResponse(
                "pay_1234567890",
                encodeOrderId(order.getId()),
                order.getOrderName(),
                "CARD",
                1000L,
                "DONE",
                "2025-11-06T00:00:00+09:00"
        );
    }

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("결제 성공 시 Payment 저장되고 OrderCompleted 이벤트가 1회 발행된다")
    void shouldSavePaymentAndPublishEvent_whenPaymentSucceeds() {
        //given
        Order order = saveOrder("test@test.com", "테스트유저");
        PaymentResponse response = buildResponse(order);

        //when
        assertDoesNotThrow(() -> paymentTxService.successPayment(response));

        //then
        Optional<Payment> payment = paymentRepository.findPaymentByPaymentKey("pay_1234567890");
        assertThat(payment).isPresent();
        assertThat(payment.get().getOrder().getId()).isEqualTo(order.getId());

        assertThat(applicationEvents.stream(OrderCompletedEvent.class).count()).isEqualTo(1);
    }

    @Test
    @DisplayName("결제 성공 후 이메일 전송 실패해도 트랜잭션은 커밋되고 이벤트는 발행된다")
    void emailFail_doesNotRollback() {
        //given
        String email = "test2@test.com";
        Order order = saveOrder(email, "테스트유저");
        PaymentResponse response = buildResponse(order);

        doThrow(new RuntimeException("메시지 발송 실패"))
                .when(emailService).sendOrderConfirmation(anyString(), anyString());

        //when
        assertDoesNotThrow(() -> paymentTxService.successPayment(response));

        //then
        Optional<Payment> payment = paymentRepository.findPaymentByPaymentKey("pay_1234567890");
        assertThat(payment).isPresent();
        assertThat(payment.get().getOrder().getId()).isEqualTo(order.getId());

        assertThat(applicationEvents.stream(OrderCompletedEvent.class).count()).isEqualTo(1);

        verify(emailService, timeout(2000))
                .sendOrderConfirmation(eq(email), eq(order.getOrderName()));
    }
}


