package com.demo_prueba.application.config;

import com.demo_prueba.domain.model.producto.gateway.ProductoGateway;
import com.demo_prueba.domain.usecase.CrearProductoUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public CrearProductoUseCase crearProductoUseCase(ProductoGateway productoGateway) {
        return new CrearProductoUseCase(productoGateway);
    }
}
