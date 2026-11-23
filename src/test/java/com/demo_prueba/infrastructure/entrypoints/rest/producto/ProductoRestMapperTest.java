package com.demo_prueba.infrastructure.entrypoints.rest.producto;

import com.demo_prueba.domain.exception.BusinessException;
import com.demo_prueba.domain.model.producto.Producto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductoRestMapperTest {

    private final ProductoRestMapper mapper = new ProductoRestMapperImpl();

    @Test
    void debeMapearRequestADominioSinId() {
        var request = CrearProductoRequest.builder()
                .nombre("Camiseta")
                .descripcion("Talla M")
                .build();

        var producto = mapper.toDomain(request);

        assertThat(producto).isEqualTo(new Producto(null, "Camiseta", "Talla M"));
    }

    @Test
    void debeMapearRequestConDescripcionNula() {
        var producto = mapper.toDomain(CrearProductoRequest.builder().nombre("Camiseta").build());

        assertThat(producto.id()).isNull();
        assertThat(producto.nombre()).isEqualTo("Camiseta");
        assertThat(producto.descripcion()).isNull();
    }

    @Test
    void debeMapearDominioAResponse() {
        var response = mapper.toResponse(new Producto("abc123", "Camiseta", "Talla M"));

        assertThat(response).isEqualTo(ProductoResponse.builder()
                .id("abc123")
                .nombre("Camiseta")
                .descripcion("Talla M")
                .build());
    }

    @Test
    void debeLanzarBusinessExceptionCuandoNombreEsInvalido() {
        var request = CrearProductoRequest.builder().nombre(" ").descripcion("Talla M").build();

        assertThatThrownBy(() -> mapper.toDomain(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("El nombre del producto es obligatorio")
                .extracting("code")
                .isEqualTo("PRODUCTO_NOMBRE_REQUERIDO");
    }

    @Test
    void debeRetornarNuloCuandoLaEntradaEsNula() {
        assertThat(mapper.toDomain(null)).isNull();
        assertThat(mapper.toResponse(null)).isNull();
    }
}
