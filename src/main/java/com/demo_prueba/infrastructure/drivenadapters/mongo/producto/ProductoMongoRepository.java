package com.demo_prueba.infrastructure.drivenadapters.mongo.producto;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface ProductoMongoRepository extends ReactiveMongoRepository<ProductoDocument, String> {
}
