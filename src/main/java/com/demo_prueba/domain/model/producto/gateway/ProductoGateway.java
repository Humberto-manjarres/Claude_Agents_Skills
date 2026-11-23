package com.demo_prueba.domain.model.producto.gateway;

import com.demo_prueba.domain.model.producto.Producto;
import reactor.core.publisher.Mono;

public interface ProductoGateway {

    Mono<Producto> guardar(Producto producto);
}
