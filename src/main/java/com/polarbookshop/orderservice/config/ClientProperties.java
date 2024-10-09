package com.polarbookshop.orderservice.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties(prefix = "polar") //@ConfigurationPropertiesScan를 @SpringBootApplication가 있는 곳에 추가해야 함
public record ClientProperties(
        @NotNull
        URI catalogServiceUri
) {
}
