package com.polarbookshop.orderservice.order.web;

import jakarta.validation.constraints.*;

//사용자로부터 주문 요청이 들어오는 것에 대한 DTO이며 validation까지 진행
public record OrderRequest(
        //null값을 가질 수 없고, 최소한 화이트 스페이스가 아닌 문자를 하나 이상 가져야 한다.
        @NotBlank(message = "The book ISBN must be defined.")
        String isbn,
        //null값을 가질수 없고 값이 1~5 사이어야 함
        @NotNull(message = "The book quantity must be defined.")
        @Min(value = 1, message = "You must order at least 1 item.")
        @Max(value = 5, message = "You cannot order more than 5 items.")
        Integer quantity
) {
}
