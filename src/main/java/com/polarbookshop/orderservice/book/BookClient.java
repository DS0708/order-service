package com.polarbookshop.orderservice.book;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/*
* WebClinet 빈의 Fluent API를 통해 catalog-service의 GET /books/{bookIsbn} 엔드포인트로 HTTP 요청을 보내는 BookClient 생성.
* 최종적으로는 Mono 퍼블리셔로 포장된 Book 객체를 반환
* */

@Component
public class BookClient {
    private static final String BOOKS_ROOT_API = "/books/";
    private final WebClient webClient; //ClientConfig에서 설정된 WebClinet 빈

    public BookClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<Book> getBookByIsbn(String isbn) {
        return webClient
                .get()
                .uri(BOOKS_ROOT_API + isbn)
                .retrieve() //요청을 보내고 응답을 받음
                .bodyToMono(Book.class); //받은 객체를 Mono<book>으로 반환
    }
}
