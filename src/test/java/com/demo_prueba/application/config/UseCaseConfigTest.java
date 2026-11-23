package com.demo_prueba.application.config;

import com.demo_prueba.domain.model.producto.Producto;
import com.demo_prueba.domain.model.producto.gateway.ProductoGateway;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UseCaseConfigTest {

    @Test
    void debeCrearCasoDeUsoConElGatewayInyectado() {
        var gateway = mock(ProductoGateway.class);
        var producto = new Producto(null, "Camiseta", null);
        var guardado = new Producto("abc123", "Camiseta", null);
        when(gateway.guardar(producto)).thenReturn(Mono.just(guardado));

        var useCase = new UseCaseConfig().crearProductoUseCase(gateway);

        StepVerifier.create(useCase.ejecutar(producto))
                .expectNext(guardado)
                .verifyComplete();
        verify(gateway).guardar(producto);
    }
}
