package com.polarbookshop.orderservice.order.domain;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface OrderRepository extends ReactiveCrudRepository<Order,Long> {
    //CRUD 연산을 제공하는 리액티브 repository가 관리할 엔티티의 유형(order)과 해당 엔티티의 primary key 유형(Long)을 지정하고 확장
}
