package com.polarbookshop.orderservice.order.event;

import com.polarbookshop.orderservice.order.domain.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

@Configuration
public class OrderFunctions {

    private static final Logger log = LoggerFactory.getLogger(OrderFunctions.class);

    @Bean
    public Consumer<Flux<OrderDispatchedMessage>> dispatchOrder( //order-service는 리액티브 애플리케이션이므로 메시지를 리액티브 스트림(Flux)으로 사용
            OrderService orderService
    ){
        return flux ->
                orderService.consumeOrderDispatchedEvent(flux) //발송된 각 메시지에 대해 DB에 해당 주문을 업데이트
                        //DB에서 업데이트된 각 주문에 대해 로그 기록
                        .doOnNext(order -> log.info("The order with id {} is dispatched", order.id()))
                        .subscribe();
        /*
        * 리액티브 스트림은 데이터를 수신할 구독자가 있는 경우에만 활성화되기 때문에,
        * 마지막에 리액티브 스트림을 subscribe하지 않으면 데이터가 처리되지 않는다.
        * */
    }
}
