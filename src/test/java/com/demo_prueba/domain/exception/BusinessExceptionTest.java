package com.demo_prueba.domain.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessExceptionTest {

    @Test
    void debeExponerCodigoYMensaje() {
        var exception = new BusinessException("CODIGO_PRUEBA", "Mensaje de prueba");

        assertThat(exception)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Mensaje de prueba");
        assertThat(exception.getCode()).isEqualTo("CODIGO_PRUEBA");
    }
}
