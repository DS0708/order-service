package com.polarbookshop.orderservice.order.domain;

import com.polarbookshop.orderservice.book.Book;
import com.polarbookshop.orderservice.book.BookClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final BookClient bookClient;

    public OrderService(OrderRepository orderRepository, BookClient bookClient) {
        this.orderRepository = orderRepository;
        this.bookClient = bookClient;
    }

    public Flux<Order> getAllOrders() { //Flux는 여러 개의 주문을 위해 사용
        return orderRepository.findAll();
    }

    //catalog-service에 연결하기 전에 임의로 주문하는 함수를 작성
    public Mono<Order> submitOrder(String isbn, int quantity){
        return bookClient.getBookByIsbn(isbn)
                .map(book -> buildAcceptedOrder(book,quantity)) //책 주문이 가능하면 접수
                .defaultIfEmpty( //책이 catalog에 존재하지 않으면 주문을 거부
                        buildRejectedOrder(isbn,quantity)
                )
                .flatMap(orderRepository::save); //주문을 접수 혹은 거부 상태로 저장
    }

    public static Order buildAcceptedOrder(Book book, int quantity){
        return Order.of(book.isbn(), book.title() + " - " + book.author(), book.price(), quantity, OrderStatus.ACCEPTED);
    }
    public static Order buildRejectedOrder(String bookIsbn, int quantity){
        return Order.of(bookIsbn, null, null, quantity, OrderStatus.REJECTED);
    }
}
