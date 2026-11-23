package com.demo_prueba.infrastructure.entrypoints.rest.producto;

import com.demo_prueba.domain.usecase.CrearProductoUseCase;
import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Component
public class ProductoHandler {

    private final CrearProductoUseCase crearProductoUseCase;
    private final ProductoRestMapper mapper;

    public ProductoHandler(CrearProductoUseCase crearProductoUseCase, ProductoRestMapper mapper) {
        this.crearProductoUseCase = crearProductoUseCase;
        this.mapper = mapper;
    }

    public Mono<ServerResponse> crear(ServerRequest request) {
        return request.bodyToMono(CrearProductoRequest.class)
                .switchIfEmpty(Mono.error(() ->
                        new ServerWebInputException("El cuerpo de la solicitud es obligatorio")))
                .map(mapper::toDomain)
                .flatMap(crearProductoUseCase::ejecutar)
                .map(mapper::toResponse)
                .flatMap(response -> ServerResponse.created(ubicacion(response.id()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response));
    }

    private static URI ubicacion(String id) {
        return UriComponentsBuilder.fromPath(ProductoRouter.PRODUCTOS_PATH)
                .pathSegment("{id}")
                .buildAndExpand(id)
                .encode()
                .toUri();
    }
}
