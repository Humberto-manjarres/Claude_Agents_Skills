package com.demo_prueba.infrastructure.drivenadapters.mongo.producto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "productos")
public class ProductoDocument {

    @Id
    private String id;
    private String nombre;
    private String descripcion;
}
