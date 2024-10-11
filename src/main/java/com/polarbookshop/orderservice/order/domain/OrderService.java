package com.polarbookshop.orderservice.order.domain;

import com.polarbookshop.orderservice.book.Book;
import com.polarbookshop.orderservice.book.BookClient;
import com.polarbookshop.orderservice.order.event.OrderAcceptedMessage;
import com.polarbookshop.orderservice.order.event.OrderDispatchedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final BookClient bookClient;
    private final StreamBridge streamBridge;

    public OrderService(OrderRepository orderRepository, BookClient bookClient, StreamBridge streamBridge) {
        this.orderRepository = orderRepository;
        this.bookClient = bookClient;
        this.streamBridge = streamBridge;
    }

    public Flux<Order> getAllOrders() { //Flux는 여러 개의 주문을 위해 사용
        return orderRepository.findAll();
    }

    //'order-accepted' 라는 채널에 메시지 큐잉
    @Transactional //매서드를 로컬 트랜잭션으로 실행
    public Mono<Order> submitOrder(String isbn, int quantity){
        return bookClient.getBookByIsbn(isbn)
                .map(book -> buildAcceptedOrder(book,quantity)) //책 주문이 가능하면 접수
                .defaultIfEmpty( //책이 catalog에 존재하지 않으면 주문을 거부
                        buildRejectedOrder(isbn,quantity)
                )
                .flatMap(orderRepository::save) //주문을 접수 혹은 거부 상태로 저장
                .doOnNext(this::publishOrderAcceptedEvent); //주문이 접수되면 이벤트 발행
    }
    private void publishOrderAcceptedEvent(Order order){
        if(!order.status().equals(OrderStatus.ACCEPTED)){
            return; //주문의 상태가 ACCEPTED가 아니면(REJECTED or DISPATCHED라면), 아무것도 하지 않음.
        }
        var orderAcceptedMessage = new OrderAcceptedMessage(order.id());
        log.info("Sending order accepted event with id: {}", order.id());
        //메시지를 acceptOrder-out-0 바인딩에 명시적으로 보낸다. 실제 RabbitMQ에는 order-accepted라는 이름의 채널로 보내는 것
        var result = streamBridge.send("acceptOrder-out-0", orderAcceptedMessage);
        log.info("Result of sending data for order with id {}; {}", order.id(), result);
    }
    //'order-dispatched'라는 채널을 구독하여 생기는 메시지에 대한 소비
    public Flux<Order> consumeOrderDispatchedEvent(
            Flux<OrderDispatchedMessage> flux //OrderDispatchedMessage의 리액티브 스트림을 입력으로 받는다.
    ){
        return flux
                .flatMap(message -> orderRepository.findById(message.orderId()))
                .map(this::buildDispatchedOrder)
                .flatMap(orderRepository::save);
    }
    private Order buildDispatchedOrder(Order existingOrder){
        return new Order(
                existingOrder.id(),
                existingOrder.bookIsbn(),
                existingOrder.bookName(),
                existingOrder.bookPrice(),
                existingOrder.quantity(),
                OrderStatus.DISPATCHED,
                existingOrder.createdDate(),
                existingOrder.lastModifiedDate(),
                existingOrder.version()
        );
    }

    public static Order buildAcceptedOrder(Book book, int quantity){
        return Order.of(book.isbn(), book.title() + " - " + book.author(), book.price(), quantity, OrderStatus.ACCEPTED);
    }
    public static Order buildRejectedOrder(String bookIsbn, int quantity){
        return Order.of(bookIsbn, null, null, quantity, OrderStatus.REJECTED);
    }
}
