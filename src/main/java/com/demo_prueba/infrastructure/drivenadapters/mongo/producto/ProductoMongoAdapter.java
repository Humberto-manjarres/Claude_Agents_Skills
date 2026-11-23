package com.demo_prueba.infrastructure.drivenadapters.mongo.producto;

import com.demo_prueba.domain.model.producto.Producto;
import com.demo_prueba.domain.model.producto.gateway.ProductoGateway;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class ProductoMongoAdapter implements ProductoGateway {

    private final ProductoMongoRepository repository;
    private final ProductoDocumentMapper mapper;

    public ProductoMongoAdapter(ProductoMongoRepository repository, ProductoDocumentMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Mono<Producto> guardar(Producto producto) {
        return Mono.fromSupplier(() -> mapper.toDocument(producto))
                .flatMap(repository::insert)
                .map(mapper::toDomain);
    }
}
