package com.demo_prueba.infrastructure.entrypoints.rest.producto;

import lombok.Builder;

@Builder
public record CrearProductoRequest(String nombre, String descripcion) {
}
