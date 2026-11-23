package com.demo_prueba.infrastructure.entrypoints.rest.producto;

import lombok.Builder;

@Builder
public record ProductoResponse(String id, String nombre, String descripcion) {
}
