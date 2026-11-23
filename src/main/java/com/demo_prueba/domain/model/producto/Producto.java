package com.demo_prueba.domain.model.producto;

import com.demo_prueba.domain.exception.BusinessException;

public record Producto(String id, String nombre, String descripcion) {

    public Producto {
        if (nombre == null || nombre.isBlank()) {
            throw new BusinessException("PRODUCTO_NOMBRE_REQUERIDO", "El nombre del producto es obligatorio");
        }
    }
}
