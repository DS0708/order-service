package com.polarbookshop.orderservice.book;

/*
* DTO로 사용할 Book 레코드이다.
* catalog-service와 주고받는 데이터이며, 두 서비스 모두 해당 클래스를 가지고 있는 클래스 중복 방법을 사용한다.(공유 라이브러리 생성 방법도 있음)
* 실제 시나리오라면 catalog-service에 새로운 EndPoint를 추가하고 이 DTO를 통해 모델링된 Book 객체를 반환해야하지만,
* 지금은 편의를 위해 기존 /books/{bookIsbn} EndPoint를 사용한다.
* 따라서 이 EndPoint를 호출해서 받은 JSON 응답을 order-service의 DTO 클래스로 역직렬화할 때 매핑되지 않는 JSON 필드는 무시하고 버린다.
* 참고로 catalog-service의 Book 객체와의 해당 필드명은 반드시 같도록 해야하며, 그렇지 않으면 데이터를 받을 수 없다.
* 소비자 중심 계약과 같이 호출된 API가 언제 변경되는지 자동 테스트를 통해 확인할 수 있는데 Spring Cloud Contract 프로젝트를 확인하면 된다.
* */
public record Book (
        String isbn,
        String title,
        String author,
        Double price
){
}
