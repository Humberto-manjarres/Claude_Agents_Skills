---
name: webflux-development
description: Patrones de implementación reactiva con Spring WebFlux, Reactor y MongoDB reactivo para este proyecto hexagonal (domain/model/<contexto>/gateway, domain/usecase, infrastructure/entrypoints/rest, infrastructure/drivenadapters, application/config). Úsalo al crear modelos, gateways, casos de uso, driven adapters, routers/handlers, configuración de beans, manejo de errores o pruebas con StepVerifier/WebTestClient.
---

# WebFlux Development

Patrones de referencia por capa. `CLAUDE.md` siempre prevalece: este skill no autoriza crear contextos, paquetes ni clases sin aprobación. Los ejemplos usan el contexto `libro` solo como ilustración.

Paquete base: `com.demo_prueba`.

---

## 1. Modelo del dominio — `domain/model/<contexto>`

- Java puro: sin Spring, Lombok, Jackson ni anotaciones de Mongo.
- Prefiere `record` para entidades inmutables y objetos de valor.
- Valida invariantes en el constructor compacto lanzando excepciones de `domain/exception`.
- Usa métodos que devuelven una copia nueva en lugar de setters.

```java
package com.demo_prueba.domain.model.libro;

import com.demo_prueba.domain.exception.BusinessException;

public record Libro(String id, String titulo, String autor) {

    public Libro {
        if (titulo == null || titulo.isBlank()) {
            throw new BusinessException("LIBRO_TITULO_REQUERIDO", "El título es obligatorio");
        }
    }

    public Libro conTitulo(String nuevoTitulo) {
        return new Libro(id, nuevoTitulo, autor);
    }
}
```

## 2. Gateway del contexto — `domain/model/<contexto>/gateway`

- Interfaz con tipos del dominio y retorno `Mono`/`Flux`.
- Sin tipos técnicos en la firma (`Document`, `ObjectId`, `ResponseEntity`, `Pageable`).

```java
package com.demo_prueba.domain.model.libro.gateway;

import com.demo_prueba.domain.model.libro.Libro;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface LibroGateway {
    Mono<Libro> guardar(Libro libro);
    Mono<Libro> buscarPorId(String id);
    Flux<Libro> listar();
    Mono<Void> eliminar(String id);
}
```

## 3. Excepciones — `domain/exception`

- Una excepción base de negocio con código estable; subclases solo si aportan semántica.

```java
package com.demo_prueba.domain.exception;

public class BusinessException extends RuntimeException {
    private final String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
```

## 4. Casos de uso — `domain/usecase`

- Clase Java pura, sin `@Service`. Se registra como bean en `application/config`.
- Una responsabilidad por caso de uso (o un caso de uso por contexto si son operaciones pequeñas; decide con el usuario).
- Depende de la interfaz del gateway, nunca de la implementación.

```java
package com.demo_prueba.domain.usecase;

import com.demo_prueba.domain.exception.BusinessException;
import com.demo_prueba.domain.model.libro.Libro;
import com.demo_prueba.domain.model.libro.gateway.LibroGateway;
import reactor.core.publisher.Mono;

public class ConsultarLibroUseCase {

    private final LibroGateway libroGateway;

    public ConsultarLibroUseCase(LibroGateway libroGateway) {
        this.libroGateway = libroGateway;
    }

    public Mono<Libro> ejecutar(String id) {
        return libroGateway.buscarPorId(id)
                .switchIfEmpty(Mono.error(() ->
                        new BusinessException("LIBRO_NO_ENCONTRADO", "No existe el libro " + id)));
    }
}
```

## 5. Configuración — `application/config`

- Registra los casos de uso como `@Bean`. Spring inyecta la implementación del gateway (el adapter anotado con `@Repository`/`@Component`), así `config` solo conoce `usecase` y la interfaz del gateway.
- Propiedades tipadas con `@ConfigurationProperties` + `record`.

```java
package com.demo_prueba.application.config;

import com.demo_prueba.domain.model.libro.gateway.LibroGateway;
import com.demo_prueba.domain.usecase.ConsultarLibroUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public ConsultarLibroUseCase consultarLibroUseCase(LibroGateway libroGateway) {
        return new ConsultarLibroUseCase(libroGateway);
    }
}
```

## 6. Driven adapters — `infrastructure/drivenadapters`

Los subpaquetes (por ejemplo `mongo`) requieren aprobación. Cuando estén aprobados:

- `@Document` + `ReactiveMongoRepository` quedan dentro del adapter; nunca salen de él.
- El adapter implementa el gateway y mapea documento ↔ dominio.
- Traduce errores técnicos a excepciones de dominio solo cuando tenga significado de negocio (p. ej. `DuplicateKeyException`).

```java
@Document(collection = "libros")
record LibroDocument(@Id String id, String titulo, String autor) { }

interface LibroMongoRepository extends ReactiveMongoRepository<LibroDocument, String> { }

@Repository
public class LibroMongoAdapter implements LibroGateway {

    private final LibroMongoRepository repository;

    public LibroMongoAdapter(LibroMongoRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Libro> buscarPorId(String id) {
        return repository.findById(id).map(LibroMongoAdapter::toDomain);
    }

    @Override
    public Mono<Libro> guardar(Libro libro) {
        return repository.save(toDocument(libro)).map(LibroMongoAdapter::toDomain);
    }

    // listar(), eliminar() ...

    private static Libro toDomain(LibroDocument d) {
        return new Libro(d.id(), d.titulo(), d.autor());
    }

    private static LibroDocument toDocument(Libro l) {
        return new LibroDocument(l.id(), l.titulo(), l.autor());
    }
}
```

### Clientes REST (WebClient)

- Construye el `WebClient` en `application/config` (base URL y timeouts desde properties).
- En el adapter: `retrieve()`, `onStatus(...)` para mapear errores, `.timeout(...)`, `retryWhen(Retry.backoff(...))` solo para errores transitorios (5xx, timeout), nunca para 4xx.
- Nunca `block()`.

## 7. Entrypoints — `infrastructure/entrypoints/rest`

Preferir **endpoints funcionales** (Router + Handler) salvo que el usuario apruebe `@RestController`.

- DTOs de entrada/salida como `record`, separados del modelo.
- El handler solo: lee la petición, valida forma, llama al caso de uso, mapea a DTO y construye la respuesta.
- Sin lógica de negocio ni acceso a repositorios.

```java
@Configuration
public class LibroRouter {
    @Bean
    public RouterFunction<ServerResponse> libroRoutes(LibroHandler handler) {
        return RouterFunctions.route()
                .GET("/api/v1/libros/{id}", handler::consultar)
                .build();
    }
}

@Component
public class LibroHandler {

    private final ConsultarLibroUseCase consultarLibroUseCase;

    public LibroHandler(ConsultarLibroUseCase consultarLibroUseCase) {
        this.consultarLibroUseCase = consultarLibroUseCase;
    }

    public Mono<ServerResponse> consultar(ServerRequest request) {
        return consultarLibroUseCase.ejecutar(request.pathVariable("id"))
                .map(LibroResponse::fromDomain)
                .flatMap(body -> ServerResponse.ok().bodyValue(body));
    }
}

public record LibroResponse(String id, String titulo, String autor) {
    static LibroResponse fromDomain(Libro l) {
        return new LibroResponse(l.id(), l.titulo(), l.autor());
    }
}
```

### Manejo de errores

- Un manejador global en `entrypoints/rest` (p. ej. extendiendo `AbstractErrorWebExceptionHandler` o un `WebExceptionHandler` con `@Order(-2)`) que traduce `BusinessException` a una respuesta uniforme (`ProblemDetail` o DTO de error) con código HTTP acordado.
- Mapeo de códigos de negocio → HTTP debe acordarse con el usuario; no inventarlo.
- Nunca exponer stack traces ni mensajes técnicos.

## 8. Operadores Reactor: guía rápida

| Situación | Usar | Evitar |
|---|---|---|
| Transformación síncrona | `map` | `flatMap` innecesario |
| Llamada que devuelve `Mono`/`Flux` | `flatMap` / `concatMap` (orden) | `map` que deja `Mono<Mono<T>>` |
| Valor ausente → error | `switchIfEmpty(Mono.error(() -> ...))` | `switchIfEmpty(Mono.error(new ...))` (crea la excepción siempre) |
| Valor por defecto costoso | `switchIfEmpty(Mono.defer(...))` | `defaultIfEmpty(calcular())` |
| Combinar independientes | `Mono.zip(a, b)` | `flatMap` anidados en serie |
| Efectos secundarios (log, métricas) | `doOnNext`, `doOnError` | lógica de negocio dentro de `doOn*` |
| Traducir errores | `onErrorMap` | `onErrorResume` que oculta errores |
| Resultado vacío tras operación | `then()` / `thenReturn(x)` | `map(x -> null)` |
| Código bloqueante | `fromCallable(...).subscribeOn(Schedulers.boundedElastic())` | llamarlo directo en el event loop |

## 9. Pruebas

Ubicación: `src/test/java/com/demo_prueba/...` replicando el paquete de la clase probada (requiere aprobación si la carpeta no existe).

### Caso de uso (unitaria, Mockito + StepVerifier)

```java
@ExtendWith(MockitoExtension.class)
class ConsultarLibroUseCaseTest {

    @Mock LibroGateway libroGateway;
    @InjectMocks ConsultarLibroUseCase useCase;

    @Test
    void debeRetornarLibroCuandoExiste() {
        var libro = new Libro("1", "Rayuela", "Cortázar");
        when(libroGateway.buscarPorId("1")).thenReturn(Mono.just(libro));

        StepVerifier.create(useCase.ejecutar("1"))
                .expectNext(libro)
                .verifyComplete();
    }

    @Test
    void debeFallarCuandoNoExiste() {
        when(libroGateway.buscarPorId("9")).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar("9"))
                .expectErrorSatisfies(e -> assertThat(e)
                        .isInstanceOf(BusinessException.class)
                        .extracting("code").isEqualTo("LIBRO_NO_ENCONTRADO"))
                .verify();
    }
}
```

### Entrypoint (WebTestClient)

- `WebTestClient.bindToRouterFunction(router.libroRoutes(handler)).build()` con el caso de uso mockeado.
- Verifica status, cuerpo y caso de error.

### Driven adapter

- Unitaria con el repositorio mockeado para el mapeo.
- Integración con Mongo (Testcontainers o embebido) solo si el usuario la aprueba: añade dependencias.

### Reglas

- Cubre camino feliz, vacío y error en cada flujo.
- Nunca `block()` en pruebas; usa `StepVerifier`.
- Para tiempo/retries: `StepVerifier.withVirtualTime`.

## 10. Checklist antes de entregar

- [ ] La fase ejecutada está aprobada y no se adelantó otra.
- [ ] Las clases están en el paquete que indica `CLAUDE.md`.
- [ ] `domain` no importa Spring, Mongo, Jackson ni Lombok.
- [ ] Ningún `block()`, `subscribe()`, `Thread.sleep()` en código productivo.
- [ ] Inyección por constructor.
- [ ] Pruebas con `StepVerifier`/`WebTestClient` para lo implementado.
- [ ] `gradlew test` ejecutado y resultado reportado tal cual.
