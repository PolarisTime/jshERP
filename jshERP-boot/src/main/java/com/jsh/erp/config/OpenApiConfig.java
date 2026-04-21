package com.jsh.erp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
                .title("管伊佳ERP Restful Api")
                .description("管伊佳ERP接口描述")
                .version("3.0")
                .contact(new Contact().name("jishenghua")));
    }
}
