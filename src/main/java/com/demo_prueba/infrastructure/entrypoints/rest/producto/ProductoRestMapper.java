package com.demo_prueba.infrastructure.entrypoints.rest.producto;

import com.demo_prueba.domain.model.producto.Producto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ProductoRestMapper {

    @Mapping(target = "id", ignore = true)
    Producto toDomain(CrearProductoRequest request);

    ProductoResponse toResponse(Producto producto);
}
