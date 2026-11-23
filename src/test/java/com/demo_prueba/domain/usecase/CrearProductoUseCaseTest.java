package com.demo_prueba.domain.usecase;

import com.demo_prueba.domain.model.producto.Producto;
import com.demo_prueba.domain.model.producto.gateway.ProductoGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrearProductoUseCaseTest {

    @Mock
    private ProductoGateway productoGateway;

    @InjectMocks
    private CrearProductoUseCase useCase;

    @Test
    void debeGuardarProductoYRetornarElProductoPersistido() {
        var producto = new Producto(null, "Camiseta", "Talla M");
        var guardado = new Producto("abc123", "Camiseta", "Talla M");
        when(productoGateway.guardar(producto)).thenReturn(Mono.just(guardado));

        StepVerifier.create(useCase.ejecutar(producto))
                .expectNext(guardado)
                .verifyComplete();

        verify(productoGateway).guardar(producto);
    }

    @Test
    void debePropagarErrorDelGateway() {
        var producto = new Producto(null, "Camiseta", "Talla M");
        var error = new IllegalStateException("fallo de persistencia");
        when(productoGateway.guardar(producto)).thenReturn(Mono.error(error));

        StepVerifier.create(useCase.ejecutar(producto))
                .expectErrorSatisfies(ex -> assertThat(ex).isSameAs(error))
                .verify();
    }

    @Test
    void debeCompletarVacioSiElGatewayNoRetornaProducto() {
        var producto = new Producto(null, "Camiseta", null);
        when(productoGateway.guardar(producto)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(producto))
                .verifyComplete();
    }
}
