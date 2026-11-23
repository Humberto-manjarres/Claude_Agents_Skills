package com.demo_prueba.domain.usecase;

import com.demo_prueba.domain.model.producto.Producto;
import com.demo_prueba.domain.model.producto.gateway.ProductoGateway;
import reactor.core.publisher.Mono;

public class CrearProductoUseCase {

    private final ProductoGateway productoGateway;

    public CrearProductoUseCase(ProductoGateway productoGateway) {
        this.productoGateway = productoGateway;
    }

    public Mono<Producto> ejecutar(Producto producto) {
        return productoGateway.guardar(producto);
    }
}
