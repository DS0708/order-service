package com.polarbookshop.orderservice.order.domain;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class OrderService {
    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {this.orderRepository = orderRepository;}

    public Flux<Order> getAllOrders() { //Flux는 여러 개의 주문을 위해 사용
        return orderRepository.findAll();
    }

    //catalog-service에 연결하기 전에 임의로 주문하는 함수를 작성
    public Mono<Order> submitOrder(String isbn, int quantity){
        // Mono 객체를 사용해 리액티브 스트림을 만들고 flatMap() 연산자를 통해 데이터를 OrderRepository에 전달
        return Mono.just(buildRejectedOrder(isbn, quantity)).flatMap(orderRepository::save);
        //flatMap 대신 map 사용시 Mono<Mono<Order>> 반환
    }
    //catalog-service에 연결하기 전이라 모든 주문은 REJECTED인 OrderStatus를 반환하도록 설정
    public static Order buildRejectedOrder(String bookIsbn, int quantity){
        return Order.of(bookIsbn, null, null, quantity, OrderStatus.REJECTED);
    }
}
