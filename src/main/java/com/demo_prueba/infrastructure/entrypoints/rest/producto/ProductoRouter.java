package com.demo_prueba.infrastructure.entrypoints.rest.producto;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class ProductoRouter {

    public static final String PRODUCTOS_PATH = "/api/v1/productos";

    @Bean
    public RouterFunction<ServerResponse> productoRoutes(ProductoHandler handler) {
        return RouterFunctions.route()
                .POST(PRODUCTOS_PATH, handler::crear)
                .build();
    }
}
