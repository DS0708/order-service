package com.polarbookshop.orderservice.order.web;

import com.polarbookshop.orderservice.order.domain.Order;
import com.polarbookshop.orderservice.order.domain.OrderService;
import com.polarbookshop.orderservice.order.domain.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/*
* OrderController에 대한 모든 Slice Test를 작성.
* 전체 Spring Application Context를 로드하는 것이 아닌,
* 특정 컨트롤러(OrderController)에 대해 제한된 Context를 생성.
* 따라서 테스트에 필요한 나머지 의존성들은 @MockBean을 통해 주입.
* */

@WebFluxTest(OrderController.class)  // OrderController를 대상으로 한 Spring WebFlux 컴포넌트에 집중하는 테스트 클래스
public class OrderControllerWebFluxTests {

    //WebClient의 변형으로 Restful 서비스 테스트를 쉽게 하기 위한 기능을 추가로 지니고 있음
    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private OrderService orderService;

    @Test
    void whenBookNotAvailableThenRejectOrder(){
        var orderRequest = new OrderRequest("1234567890",3);
        var expectedOrder = OrderService.buildRejectedOrder(orderRequest.isbn(), orderRequest.quantity());
        // MockBean이 어떻게 작동해야 하는지 지정
        given(orderService.submitOrder(orderRequest.isbn(), orderRequest.quantity()))
                .willReturn(Mono.just(expectedOrder));

        webTestClient
                .post()
                .uri("/orders")
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus().is2xxSuccessful() //주문이 성공적으로 생성될 것을 예상
                .expectBody(Order.class).value(actualOrder -> {
                    assertThat(actualOrder).isNotNull();
                    assertThat(actualOrder.status()).isEqualTo(OrderStatus.REJECTED);
                });
    }
}
