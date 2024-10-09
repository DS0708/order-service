package com.polarbookshop.orderservice.order.domain;

import com.polarbookshop.orderservice.config.DataConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.test.StepVerifier;

//테스트 컨테이너에 기반한 Slice Test로 도커 엔진이 로컬 환경에서 실행중이어야 한다.

@DataR2dbcTest //R2DBC 컴포넌트에 집중하는 테스트 클래스
@Import(DataConfig.class) // auditing 활성화
@Testcontainers //테스트컨테이너의 자동 시작과 중지를 활성화
public class OrderRepositoryR2dbcTests {

    @Container //테스트를 위한 PostgreSQL 컨테이너를 시작하고 이를 식별
    static PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"));

    @Autowired
    private OrderRepository orderRepository;

    @DynamicPropertySource //테스트 PostgreSQL 인스턴스에 연결하도록 R2DBC와 Flyway 설정을 변경
    static void postgresqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", OrderRepositoryR2dbcTests::r2dbcUrl);
        registry.add("spring.r2dbc.username", postgresql::getUsername);
        registry.add("spring.r2dbc.password", postgresql::getPassword);
        registry.add("spring.flyway.url", postgresql::getJdbcUrl);
    }

    //테스트 컨테이너가 JDBC와는 다르게 R2DBC에 대해서는 연결 문자열을 제공하지 않기 때문에 연결 문자열을 생성
    private static String r2dbcUrl() {
        return String.format("r2dbc:postgresql://%s:%s/%s", postgresql.getHost(),
                postgresql.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT), postgresql.getDatabaseName());
    }

    @Test
    void createRejectedOrder(){
        var rejectedOrder = OrderService.buildRejectedOrder("1234567890",3);
        StepVerifier
                .create(orderRepository.save(rejectedOrder))    //OrderRepository가 반환하는 객체로 StepVerifier를 초기화
                .expectNextMatches(order -> order.status().equals(OrderStatus.REJECTED))
                .verifyComplete(); //Reactive Stream이 성공적으로 완료됐는지 확인
    }
}
