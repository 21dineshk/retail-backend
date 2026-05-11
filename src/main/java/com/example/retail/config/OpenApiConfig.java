package com.example.retail.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI retailOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Retail Backend API")
                .description("Sample retail storefront backend used to exercise MCP-Forge tool generation. "
                    + "All endpoints are user-scoped via path parameter (no authentication).")
                .version("0.1.0")
                .contact(new Contact().name("retail-backend").email("dev@example.com")))
            .servers(List.of(
                new Server().url("http://localhost:8090").description("Local"),
                new Server().url("http://host.docker.internal:8090").description("From inside a Docker container on the same Mac")
            ));
    }
}
