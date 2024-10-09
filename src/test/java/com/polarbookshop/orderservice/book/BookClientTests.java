package com.polarbookshop.orderservice.book;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;

public class BookClientTests {
    private MockWebServer mockWebServer;
    private BookClient bookClient;

    @BeforeEach
    void setUp() throws IOException {
        this.mockWebServer = new MockWebServer();
        this.mockWebServer.start(); //테스트를 위한 모의 서버 시작
        var webClient = WebClient.builder()
                .baseUrl(this.mockWebServer.url("/").toString())
                .build();
        this.bookClient = new BookClient(webClient);
    }

    @AfterEach
    void clean() throws IOException {
        this.mockWebServer.shutdown(); //테스트가 끝나면 모의서버를 중지
    }

    @Test
    void whenBookExistsThenReturnBook() {
        var bookIsbn = "1234567890";

        var mockResponse = new MockResponse() //모의 서버에 의해 반환되는 응답을 정의
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                    {
                        "isbn": %s,
                        "title": "Title",
                        "author": "Author",
                        "price": 9.90,
                        "publisher": "Polarsophia"
                    }
                """.formatted(bookIsbn));

        mockWebServer.enqueue(mockResponse); //모의 서버가 처리하는 큐에 모의 응답 추가

        Mono<Book> book = bookClient.getBookByIsbn(bookIsbn);

        //StepVerifier를 상요하면 리액티브 스트림을 처리하고 Fluent API를 통해 assertion을 단계별로 실행해 각각의 작동을 테스트할 수 있음.
        StepVerifier.create(book)   //BookClient가 반환하는 객체로 StepVerifier 객체를 초기화
                .expectNextMatches(b -> b.isbn().equals(bookIsbn))
                .verifyComplete();  //Reactive Stream이 성공적으로 완료됐는지 확인
    }
}
