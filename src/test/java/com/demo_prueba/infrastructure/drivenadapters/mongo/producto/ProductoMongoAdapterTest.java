package com.demo_prueba.infrastructure.drivenadapters.mongo.producto;

import com.demo_prueba.domain.model.producto.Producto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoMongoAdapterTest {

    @Mock
    private ProductoMongoRepository repository;

    private ProductoMongoAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ProductoMongoAdapter(repository, new ProductoDocumentMapperImpl());
    }

    @Test
    void debeInsertarYMapearElDocumentoPersistido() {
        var producto = new Producto(null, "Camiseta", "Talla M");
        var persistido = ProductoDocument.builder()
                .id("abc123")
                .nombre("Camiseta")
                .descripcion("Talla M")
                .build();
        when(repository.insert(any(ProductoDocument.class))).thenReturn(Mono.just(persistido));

        StepVerifier.create(adapter.guardar(producto))
                .expectNext(new Producto("abc123", "Camiseta", "Talla M"))
                .verifyComplete();

        var captor = ArgumentCaptor.forClass(ProductoDocument.class);
        verify(repository).insert(captor.capture());
        assertThat(captor.getValue().getId()).isNull();
        assertThat(captor.getValue().getNombre()).isEqualTo("Camiseta");
        assertThat(captor.getValue().getDescripcion()).isEqualTo("Talla M");
        verify(repository, never()).save(any(ProductoDocument.class));
    }

    @Test
    void debePropagarErrorDelRepositorio() {
        var producto = new Producto(null, "Camiseta", "Talla M");
        var error = new DuplicateKeyException("duplicado");
        when(repository.insert(any(ProductoDocument.class))).thenReturn(Mono.error(error));

        StepVerifier.create(adapter.guardar(producto))
                .expectErrorSatisfies(ex -> assertThat(ex).isSameAs(error))
                .verify();

        verify(repository, never()).save(any(ProductoDocument.class));
    }

    @Test
    void debeCompletarVacioSiElRepositorioNoRetornaDocumento() {
        when(repository.insert(any(ProductoDocument.class))).thenReturn(Mono.empty());

        StepVerifier.create(adapter.guardar(new Producto(null, "Camiseta", null)))
                .verifyComplete();
    }

    @Test
    void noDebeInvocarElRepositorioHastaLaSuscripcion() {
        adapter.guardar(new Producto(null, "Camiseta", null));

        verifyNoInteractions(repository);
    }
}
