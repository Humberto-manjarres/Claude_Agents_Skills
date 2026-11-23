package com.demo_prueba.infrastructure.entrypoints.rest.producto;

import com.demo_prueba.domain.model.producto.Producto;
import com.demo_prueba.domain.usecase.CrearProductoUseCase;
import com.demo_prueba.infrastructure.entrypoints.rest.GlobalErrorHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoHandlerTest {

    private static final String PATH = ProductoRouter.PRODUCTOS_PATH;

    @Mock
    private CrearProductoUseCase crearProductoUseCase;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        var handler = new ProductoHandler(crearProductoUseCase, new ProductoRestMapperImpl());
        var globalErrorHandler = new GlobalErrorHandler(ServerCodecConfigurer.create());
        // HandlerStrategies.empty() omite el WebFluxResponseStatusExceptionHandler por defecto,
        // que respondería las ResponseStatusException antes que el GlobalErrorHandler.
        var strategies = HandlerStrategies.empty()
                .codecs(codecs -> codecs.registerDefaults(true))
                .exceptionHandler(globalErrorHandler)
                .build();
        webTestClient = WebTestClient
                .bindToRouterFunction(new ProductoRouter().productoRoutes(handler))
                .handlerStrategies(strategies)
                .build();
    }

    @Test
    void debeCrearProductoYResponder201ConLocationYBody() {
        var esperado = new Producto(null, "Camiseta", "Talla M");
        when(crearProductoUseCase.ejecutar(esperado))
                .thenReturn(Mono.just(new Producto("abc123", "Camiseta", "Talla M")));

        webTestClient.post().uri(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"nombre\":\"Camiseta\",\"descripcion\":\"Talla M\"}")
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().location(PATH + "/abc123")
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id").isEqualTo("abc123")
                .jsonPath("$.nombre").isEqualTo("Camiseta")
                .jsonPath("$.descripcion").isEqualTo("Talla M");

        verify(crearProductoUseCase).ejecutar(esperado);
    }

    @Test
    void debeIgnorarIdEnviadoEnElBody() {
        var esperado = new Producto(null, "Camiseta", null);
        when(crearProductoUseCase.ejecutar(esperado))
                .thenReturn(Mono.just(new Producto("abc123", "Camiseta", null)));

        webTestClient.post().uri(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"id\":\"cliente\",\"nombre\":\"Camiseta\"}")
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().location(PATH + "/abc123")
                .expectBody()
                .jsonPath("$.id").isEqualTo("abc123")
                .jsonPath("$.nombre").isEqualTo("Camiseta");

        verify(crearProductoUseCase).ejecutar(esperado);
    }

    @Test
    void debeResponder400CuandoNombreEstaAusente() {
        webTestClient.post().uri(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"descripcion\":\"Talla M\"}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.code").isEqualTo("PRODUCTO_NOMBRE_REQUERIDO")
                .jsonPath("$.detail").isEqualTo("El nombre del producto es obligatorio")
                .jsonPath("$.instance").isEqualTo(PATH);

        verifyNoInteractions(crearProductoUseCase);
    }

    @Test
    void debeResponder400CuandoJsonEsInvalido() {
        webTestClient.post().uri(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"nombre\":")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.code").isEqualTo("SOLICITUD_INVALIDA")
                .jsonPath("$.detail").isEqualTo("El cuerpo de la solicitud es inválido o está ausente");

        verifyNoInteractions(crearProductoUseCase);
    }

    @Test
    void debeResponder400CuandoBodyEstaAusente() {
        webTestClient.post().uri(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.code").isEqualTo("SOLICITUD_INVALIDA");

        verifyNoInteractions(crearProductoUseCase);
    }

    @Test
    void debeResponder500SinDetalleTecnicoCuandoElCasoDeUsoFalla() {
        when(crearProductoUseCase.ejecutar(any(Producto.class)))
                .thenReturn(Mono.error(new IllegalStateException("conexion rechazada a mongo:27017")));

        webTestClient.post().uri(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"nombre\":\"Camiseta\"}")
                .exchange()
                .expectStatus().isEqualTo(500)
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody(String.class)
                .value(body -> assertThat(body)
                        .contains("\"code\":\"ERROR_INTERNO\"")
                        .contains("Ocurrió un error inesperado")
                        .doesNotContain("conexion rechazada")
                        .doesNotContain("mongo:27017")
                        .doesNotContain("IllegalStateException"));
    }
}
