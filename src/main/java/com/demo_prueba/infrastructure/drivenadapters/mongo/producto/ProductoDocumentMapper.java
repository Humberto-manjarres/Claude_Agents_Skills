package com.demo_prueba.infrastructure.drivenadapters.mongo.producto;

import com.demo_prueba.domain.model.producto.Producto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ProductoDocumentMapper {

    Producto toDomain(ProductoDocument document);

    ProductoDocument toDocument(Producto producto);
}
