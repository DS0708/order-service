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
        /*
        * RabbitMQ는 적어도 하나의 전달(at-least-one delivered)을 보증하기 때문에
        * 중복으로 메시지를 받을 수 있음을 유념해야 한다.
        * 따라서 연산이 멱등성(계속 실행해도 결과가 같음)을 가지도록 구현해야 한다.
        * */
        if(!order.status().equals(OrderStatus.ACCEPTED)){
            return; //주문의 상태가 ACCEPTED가 아니면(REJECTED or DISPATCHED라면), 아무것도 하지 않음.
        }
        var orderAcceptedMessage = new OrderAcceptedMessage(order.id());
        log.info("Sending order accepted event with id: {}", order.id());
        //메시지를 acceptOrder-out-0 바인딩에 명시적으로 보낸다. 실제 RabbitMQ에는 order-accepted라는 이름의 채널로 보내는 것
        var result = streamBridge.send("acceptOrder-out-0", orderAcceptedMessage);
        /*
        * 데이터의 발원지가 REST 엔드포인트이기 때문에 Spring Cloud Function에 등록할 수 있는 공급자 빈이 없고,
        * 따라서 프레임워크가 RabbitMQ에 대해 필요한 바인딩을 생성할 수 있는 수단이 없다.
        * 그래서 StreamBridge를 사용하여 'acceptOrder-out-0'라는 대상 바인딩에게 메시지를 보낸다.
        * 그리고 acceptOrder라는 함수는 없지만 애플리케이션이 시작할 떄 Spring Cloud Stream은 내부적으로
        * StreamBridge가 acceptOrder 바인딩을 통해 메시지를 발행하려는 것을 인식하고 이 바인딩 객체를 자동으로 생성한다.
        * */
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
