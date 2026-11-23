package com.demo_prueba.infrastructure.drivenadapters.mongo.producto;

import com.demo_prueba.domain.exception.BusinessException;
import com.demo_prueba.domain.model.producto.Producto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductoDocumentMapperTest {

    private final ProductoDocumentMapper mapper = new ProductoDocumentMapperImpl();

    @Test
    void debeMapearDominioADocumento() {
        var producto = new Producto("abc123", "Camiseta", "Talla M");

        var document = mapper.toDocument(producto);

        assertThat(document.getId()).isEqualTo("abc123");
        assertThat(document.getNombre()).isEqualTo("Camiseta");
        assertThat(document.getDescripcion()).isEqualTo("Talla M");
    }

    @Test
    void debeMapearDocumentoADominio() {
        var document = ProductoDocument.builder()
                .id("abc123")
                .nombre("Camiseta")
                .descripcion("Talla M")
                .build();

        var producto = mapper.toDomain(document);

        assertThat(producto).isEqualTo(new Producto("abc123", "Camiseta", "Talla M"));
    }

    @Test
    void debeConservarDatosEnIdaYVuelta() {
        var producto = new Producto("abc123", "Camiseta", "Talla M");

        assertThat(mapper.toDomain(mapper.toDocument(producto))).isEqualTo(producto);
    }

    @Test
    void debeMapearIdNuloAlCrear() {
        var document = mapper.toDocument(new Producto(null, "Camiseta", "Talla M"));

        assertThat(document.getId()).isNull();
        assertThat(document.getNombre()).isEqualTo("Camiseta");
        assertThat(document.getDescripcion()).isEqualTo("Talla M");
    }

    @Test
    void debeMapearDescripcionNula() {
        var producto = new Producto("abc123", "Camiseta", null);

        var document = mapper.toDocument(producto);

        assertThat(document.getDescripcion()).isNull();
        assertThat(mapper.toDomain(document)).isEqualTo(producto);
    }

    @Test
    void debeRetornarNuloCuandoLaEntradaEsNula() {
        assertThat(mapper.toDocument(null)).isNull();
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    void debeFallarAlMapearDocumentoSinNombre() {
        var document = ProductoDocument.builder().id("abc123").build();

        assertThatThrownBy(() -> mapper.toDomain(document))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("PRODUCTO_NOMBRE_REQUERIDO");
    }
}
