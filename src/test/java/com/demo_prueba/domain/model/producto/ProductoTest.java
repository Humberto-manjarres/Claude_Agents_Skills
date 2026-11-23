package com.demo_prueba.domain.model.producto;

import com.demo_prueba.domain.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductoTest {

    private static final String CODIGO_NOMBRE_REQUERIDO = "PRODUCTO_NOMBRE_REQUERIDO";

    @Test
    void debeCrearProductoSinIdCuandoNombreEsValido() {
        var producto = new Producto(null, "Camiseta", "Camiseta de algodon");

        assertThat(producto.id()).isNull();
        assertThat(producto.nombre()).isEqualTo("Camiseta");
        assertThat(producto.descripcion()).isEqualTo("Camiseta de algodon");
    }

    @Test
    void debeCrearProductoConId() {
        var producto = new Producto("abc123", "Camiseta", "Camiseta de algodon");

        assertThat(producto.id()).isEqualTo("abc123");
    }

    @Test
    void debePermitirDescripcionNula() {
        var producto = new Producto(null, "Camiseta", null);

        assertThat(producto.descripcion()).isNull();
        assertThat(producto.nombre()).isEqualTo("Camiseta");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t", "\n"})
    void debeFallarCuandoNombreEsNuloVacioOEnBlanco(String nombreInvalido) {
        assertThatThrownBy(() -> new Producto(null, nombreInvalido, "Descripcion"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("El nombre del producto es obligatorio")
                .extracting("code")
                .isEqualTo(CODIGO_NOMBRE_REQUERIDO);
    }
}
