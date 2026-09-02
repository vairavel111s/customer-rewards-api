package com.retailer.rewards.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Publishes the OpenAPI document served at {@code /v3/api-docs} and rendered by
 * Swagger UI at {@code /swagger-ui.html}.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI rewardsOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Customer Rewards API")
                .version("1.0.0")
                .description("Calculates retailer reward points per customer, broken down "
                        + "by calendar month and in total, over a configurable time frame.")
                .contact(new Contact().name("Rewards Platform Team"))
                .license(new License().name("MIT")));
    }
}
