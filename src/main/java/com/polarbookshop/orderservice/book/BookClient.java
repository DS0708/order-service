package com.polarbookshop.orderservice.book;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

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
                .bodyToMono(Book.class) //받은 객체를 Mono<book>으로 반환
                //실제 프로덕션 환경에서는 timout 설정을 외부화하여 애플리케이션을 다시 빌드하지 않고도 환경에 따라 값을 변경할 수 있게 함
                .timeout(Duration.ofSeconds(3), Mono.empty()) //GET 요청에 대한 타임아웃 3초로 설정, 그리고 타임아웃에 대한 폴백으로 빈 결과를 반환
                //retryWhen의 경우 404를 포함한 모든 에러에 대하여 재시도를 수행하기 때문에, 404같은 에러에 대하여 재시도를 안하도록 설정할 필요가 있음.
                .onErrorResume(WebClientResponseException.NotFound.class, exception -> Mono.empty()) //404에러를 받으면 빈 객체를 반환
                .retryWhen( //retryWhen이 timeout 뒤에 올 경우 각 재시도에 대한 timeout이 3초이며, 그 반대일 경우 요청보내고 모든 재시도까지 3초안에 해야함
                        //exponential backoff를 retry 전략으로 사용, 100밀리초의 초기 백오프로 총 3회까지 시도
                        Retry.backoff(3, Duration.ofMillis(100)) //timeout이 된 후 100ms 후에 재시도를 시도, 그 다음은 200ms
                )
                .onErrorResume(Exception.class, exception -> Mono.empty()); //3회의 재시도 동안 오류가 발생하면 예외를 포착하고 빈 객체 반환
    }
}
