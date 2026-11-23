---
name: webflux-development
description: Patrones de implementación reactiva con Spring WebFlux, Reactor, MongoDB reactivo, MapStruct y Lombok @Builder para este proyecto hexagonal (domain/model/<contexto>/gateway, domain/usecase, infrastructure/entrypoints/rest/<contexto>, infrastructure/drivenadapters/mongo/<contexto>, application/config). Úsalo al crear modelos, gateways, casos de uso, driven adapters, mappers, routers/handlers, configuración de beans y application.yml, manejo de errores con ProblemDetail o pruebas con StepVerifier/WebTestClient.
---

# WebFlux Development

Patrones de referencia por capa. `CLAUDE.md` siempre prevalece: este skill no autoriza crear contextos, paquetes ni clases sin aprobación. Los ejemplos usan el contexto `libro` solo como ilustración; la implementación real de referencia es el contexto `producto` (funcionalidad Crear Producto).

Paquete base: `com.demo_prueba`.

---

## 0. Mapa de paquetes por contexto

Cada contexto funcional aprobado (`<contexto>` en minúscula y singular) se replica así:

```
com/demo_prueba
├── application/config
│   └── UseCaseConfig.java                         ← beans de casos de uso (compartido)
├── domain
│   ├── model/<contexto>
│   │   ├── <Entidad>.java                         ← record, Java puro
│   │   └── gateway/<Entidad>Gateway.java          ← puerto de salida del contexto
│   ├── usecase/<Accion><Entidad>UseCase.java      ← plano, sin subcarpeta por contexto
│   └── exception/BusinessException.java           ← compartida
└── infrastructure
    ├── entrypoints/rest
    │   ├── GlobalErrorHandler.java                ← compartido, en la raíz de rest
    │   └── <contexto>/                            ← todo lo REST del contexto
    │       ├── <Entidad>Router.java
    │       ├── <Entidad>Handler.java
    │       ├── <Accion><Entidad>Request.java
    │       ├── <Entidad>Response.java
    │       └── <Entidad>RestMapper.java
    └── drivenadapters/mongo
        └── <contexto>/                            ← todo lo Mongo del contexto
            ├── <Entidad>Document.java
            ├── <Entidad>MongoRepository.java
            ├── <Entidad>DocumentMapper.java
            └── <Entidad>MongoAdapter.java
```

Reglas:

- Lo específico de un contexto va en su subcarpeta `<contexto>`; lo transversal (manejador global de errores, configuración) va en la raíz de su capa.
- `domain/usecase` es plano (sin subcarpetas) porque `CLAUDE.md` no las define.
- Otra tecnología de driven adapter (WebClient, Kafka) sería un hermano de `mongo` (p. ej. `drivenadapters/<tecnologia>/<contexto>`) y requiere aprobación.

### Dependencias usadas por el patrón

Además de las dependencias básicas, `CLAUDE.md` permite explícitamente estas, que el patrón necesita. Úsalas solo para lo indicado:

- `entrypoints → domain.model` — el mapper REST convierte DTO ↔ entidad.
- `drivenadapters → domain.model` — el mapper de documento convierte documento ↔ entidad.
- `config → domain.model.<contexto>.gateway` — el bean del caso de uso recibe la interfaz del gateway.

---

## 1. Modelo del dominio — `domain/model/<contexto>`

- Java puro: sin Spring, Lombok, MapStruct, Jackson ni anotaciones de Mongo (`CLAUDE.md` prohíbe frameworks en `domain/model`).
- Prefiere `record`. MapStruct construye records por su constructor canónico, así que no necesitan `@Builder`.
- Si un record requiere builder en el dominio, escríbelo a mano; Lombok `@Builder` en `domain` requiere aprobación y cambio de `CLAUDE.md`.
- Valida invariantes en el constructor compacto lanzando `BusinessException` con un código `<CONTEXTO>_<REGLA>` (p. ej. `PRODUCTO_NOMBRE_REQUERIDO`).
- Si la persistencia genera el id, el record acepta `id == null` al crear.
- Solo las reglas aprobadas: no inventar longitudes, unicidad ni campos.
- No agregar métodos (copias, `conX(...)`) que ningún caso de uso aprobado necesite.

```java
package com.demo_prueba.domain.model.libro;

import com.demo_prueba.domain.exception.BusinessException;

public record Libro(String id, String titulo, String autor) {

    public Libro {
        if (titulo == null || titulo.isBlank()) {
            throw new BusinessException("LIBRO_TITULO_REQUERIDO", "El título del libro es obligatorio");
        }
    }
}
```

## 2. Gateway del contexto — `domain/model/<contexto>/gateway`

- Interfaz con tipos del dominio y retorno `Mono`/`Flux`.
- Sin tipos técnicos en la firma (`Document`, `ObjectId`, `ResponseEntity`, `Pageable`).
- **Solo las operaciones que usa un caso de uso aprobado.** No agregar `buscarPorId`, `listar`, `eliminar`, etc. "por si acaso".

```java
package com.demo_prueba.domain.model.libro.gateway;

import com.demo_prueba.domain.model.libro.Libro;
import reactor.core.publisher.Mono;

public interface LibroGateway {
    Mono<Libro> guardar(Libro libro);
}
```

## 3. Excepciones — `domain/exception`

- Una única `BusinessException` con código estable; subclases solo si aportan semántica aprobada (p. ej. duplicado → 409).

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

- Clase Java pura, sin `@Service`, nombre `<Accion><Entidad>UseCase`, método `ejecutar(...)`.
- Un caso de uso por operación.
- Depende de la interfaz del gateway, nunca de la implementación.
- Las validaciones de forma del dato viven en la entidad; el caso de uso orquesta y aplica reglas que requieren consultar gateways.

```java
package com.demo_prueba.domain.usecase;

import com.demo_prueba.domain.model.libro.Libro;
import com.demo_prueba.domain.model.libro.gateway.LibroGateway;
import reactor.core.publisher.Mono;

public class CrearLibroUseCase {

    private final LibroGateway libroGateway;

    public CrearLibroUseCase(LibroGateway libroGateway) {
        this.libroGateway = libroGateway;
    }

    public Mono<Libro> ejecutar(Libro libro) {
        return libroGateway.guardar(libro);
    }
}
```

## 5. Configuración — `application/config` y `application.yml`

### Beans

- Registra los casos de uso como `@Bean` en `UseCaseConfig` (una sola clase compartida; agrega un método por caso de uso).
- Spring inyecta la implementación del gateway (el adapter con `@Repository`), así `config` solo conoce `usecase` y la interfaz del gateway.
- Propiedades tipadas con `@ConfigurationProperties` + `record`.

```java
@Configuration
public class UseCaseConfig {

    @Bean
    public CrearLibroUseCase crearLibroUseCase(LibroGateway libroGateway) {
        return new CrearLibroUseCase(libroGateway);
    }
}
```

### `src/main/resources/application.yml`

- El proyecto usa **YAML** (`application.yml`); no crear `application.properties`.
- **Spring Boot 4:** las propiedades de conexión de Mongo son `spring.mongodb.*`. `spring.data.mongodb.uri` / `.database` están deprecadas con nivel *error* (verificado en los metadatos de `spring-boot-mongodb` 4.2.0). Bajo `spring.data.mongodb.*` solo quedan propiedades de Spring Data (p. ej. GridFS).
- URI por variable de entorno con valor local por defecto; nunca credenciales en el archivo.
- Base de datos del proyecto: `bd_demo`.

```yaml
spring:
  application:
    name: demo_prueba
  mongodb:
    uri: ${MONGODB_URI:mongodb://localhost:27017/bd_demo}
    database: bd_demo
```

Perfiles adicionales (`application-<perfil>.yml`) requieren aprobación.

## 6. Driven adapters — `infrastructure/drivenadapters/mongo/<contexto>`

- `@Document` + `ReactiveMongoRepository` quedan dentro del paquete del contexto; nunca salen del adapter.
- El documento es una clase Lombok con `@Builder` (sección 6.1).
- El adapter implementa el gateway y delega el mapeo a un mapper MapStruct (sección 6.2).
- **Para crear usa `insert`, no `save`**: `save` hace upsert y, si llega un id existente, sobrescribe en silencio.
- Envuelve el mapeo de entrada en `Mono.fromSupplier(...)` para que un error de mapeo viaje como señal del flujo.
- Traduce errores técnicos a excepciones de dominio solo cuando tengan significado de negocio aprobado (p. ej. `DuplicateKeyException` → duplicado).
- Colección: nombre del contexto en plural (`productos`, `libros`), salvo indicación del usuario.

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "libros")
public class LibroDocument {
    @Id
    private String id;
    private String titulo;
    private String autor;
}

public interface LibroMongoRepository extends ReactiveMongoRepository<LibroDocument, String> { }

@Repository
public class LibroMongoAdapter implements LibroGateway {

    private final LibroMongoRepository repository;
    private final LibroDocumentMapper mapper;

    public LibroMongoAdapter(LibroMongoRepository repository, LibroDocumentMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Mono<Libro> guardar(Libro libro) {
        return Mono.fromSupplier(() -> mapper.toDocument(libro))
                .flatMap(repository::insert)
                .map(mapper::toDomain);
    }
}
```

### 6.1 Lombok `@Builder` en infraestructura

- Permitido en documentos y DTOs (`drivenadapters`, `entrypoints`). Prohibido en `domain` sin aprobación.
- Documentos Mongo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor` (Spring Data necesita constructor vacío o completo).
- DTOs: `record`; si necesita builder, `@Builder` sobre el record.
- `@Builder.Default` para valores por defecto; sin él, el builder ignora el inicializador del campo.
- Evitar `@Data` en clases con relaciones bidireccionales.
- En pruebas, usa el builder para armar datos.

### 6.2 MapStruct

- Los mappers viven **solo en infraestructura**, en el mismo paquete de contexto que el adapter o entrypoint que los usa.
- `componentModel = "spring"` para inyectarlos por constructor; nunca `Mappers.getMapper(...)` en código productivo.
- `unmappedTargetPolicy = ReportingPolicy.ERROR` para que un campo olvidado rompa la compilación.
- MapStruct usa el `@Builder` de Lombok del destino (gracias a `lombok-mapstruct-binding`) y el constructor canónico cuando el destino es un `record`.
- Sin lógica de negocio: solo conversión de forma. Conversiones técnicas con `@Mapping` o métodos `default`.
- Un mapper por frontera: `<Entidad>DocumentMapper` (adapter) y `<Entidad>RestMapper` (entrypoint).
- Si la persistencia genera el id: `@Mapping(target = "id", ignore = true)` en `RestMapper.toDomain`.

```java
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface LibroDocumentMapper {
    Libro toDomain(LibroDocument document);
    LibroDocument toDocument(Libro libro);
}
```

Gradle (ya configurado en `build.gradle`):

```groovy
implementation "org.mapstruct:mapstruct:${mapstructVersion}"
annotationProcessor "org.projectlombok:lombok"
annotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"
annotationProcessor "org.projectlombok:lombok-mapstruct-binding:${lombokMapstructBindingVersion}"
// y los equivalentes testAnnotationProcessor
```

Si MapStruct reporta *"Unknown property"* sobre un tipo con Lombok, falta `lombok-mapstruct-binding`.

### 6.3 Clientes REST (WebClient)

- Paquete propio hermano de `mongo` (requiere aprobación).
- El `WebClient` se construye en `application/config` (base URL y timeouts desde `application.yml`).
- En el adapter: `retrieve()`, `onStatus(...)` para mapear errores, `.timeout(...)`, `retryWhen(Retry.backoff(...))` solo para errores transitorios (5xx, timeout), nunca para 4xx.

## 7. Entrypoints — `infrastructure/entrypoints/rest/<contexto>`

Endpoints **funcionales** (Router + Handler), salvo que el usuario apruebe `@RestController`.

- Ruta: `/api/v1/<contexto en plural>`; definir el path como constante pública del Router y reutilizarla.
- DTOs de entrada/salida como `record`, separados del modelo. El request de creación no incluye `id` si lo genera la persistencia.
- Mapeo DTO ↔ dominio con `<Entidad>RestMapper` inyectado en el handler.
- El handler solo: lee el body, rechaza body ausente, mapea a dominio, llama al caso de uso, mapea a respuesta y construye el `ServerResponse`.
- La validación de negocio ocurre al construir la entidad dentro del `map`: la `BusinessException` viaja por el flujo hasta el manejador global.
- Creación: **201 Created** + header `Location` construido con `UriComponentsBuilder` (codificado) + body con el recurso.
- Sin lógica de negocio ni acceso a repositorios o `drivenadapters`.

```java
@Configuration
public class LibroRouter {

    public static final String LIBROS_PATH = "/api/v1/libros";

    @Bean
    public RouterFunction<ServerResponse> libroRoutes(LibroHandler handler) {
        return RouterFunctions.route()
                .POST(LIBROS_PATH, handler::crear)
                .build();
    }
}

@Component
public class LibroHandler {

    private final CrearLibroUseCase crearLibroUseCase;
    private final LibroRestMapper mapper;

    public LibroHandler(CrearLibroUseCase crearLibroUseCase, LibroRestMapper mapper) {
        this.crearLibroUseCase = crearLibroUseCase;
        this.mapper = mapper;
    }

    public Mono<ServerResponse> crear(ServerRequest request) {
        return request.bodyToMono(CrearLibroRequest.class)
                .switchIfEmpty(Mono.error(() ->
                        new ServerWebInputException("El cuerpo de la solicitud es obligatorio")))
                .map(mapper::toDomain)
                .flatMap(crearLibroUseCase::ejecutar)
                .map(mapper::toResponse)
                .flatMap(response -> ServerResponse.created(ubicacion(response.id()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response));
    }

    private static URI ubicacion(String id) {
        return UriComponentsBuilder.fromPath(LibroRouter.LIBROS_PATH)
                .pathSegment("{id}")
                .buildAndExpand(id)
                .encode()
                .toUri();
    }
}

public record CrearLibroRequest(String titulo, String autor) { }

@Builder
public record LibroResponse(String id, String titulo, String autor) { }

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface LibroRestMapper {

    @Mapping(target = "id", ignore = true)
    Libro toDomain(CrearLibroRequest request);

    LibroResponse toResponse(Libro libro);
}
```

### 7.1 Manejo de errores — `entrypoints/rest/GlobalErrorHandler.java`

Ya existe un único manejador global (compartido por todos los contextos). **No crear otro por contexto**; extenderlo si aparece un caso nuevo aprobado.

- `WebExceptionHandler` con `@Order(-2)` (antes del manejador por defecto de Boot, `-1`).
- Respuesta `ProblemDetail` (RFC 9457) como `application/problem+json`, con `title`, `detail`, `instance` y la propiedad adicional `code` (sale al primer nivel del JSON).
- Mapeo vigente:

| Excepción | HTTP | `code` |
|---|---|---|
| `BusinessException` | 400 | el de la excepción (p. ej. `PRODUCTO_NOMBRE_REQUERIDO`) |
| `ServerWebInputException` / `DecodingException` (JSON inválido o body ausente) | 400 | `SOLICITUD_INVALIDA` |
| Otras `ResponseStatusException` (404, 405, 415…) | se delegan al manejador por defecto de Boot (sin `ProblemDetail`), conservando su código — decisión aprobada | — |
| Cualquier otra | 500 | `ERROR_INTERNO`, mensaje genérico; detalle solo en log (SLF4J) |

- Cualquier código HTTP nuevo (404 de negocio, 409 de duplicado…) se acuerda con el usuario; no inventarlo.
- Nunca exponer stack traces ni mensajes técnicos.

## 8. Operadores Reactor: guía rápida

| Situación | Usar | Evitar |
|---|---|---|
| Transformación síncrona | `map` | `flatMap` innecesario |
| Llamada que devuelve `Mono`/`Flux` | `flatMap` / `concatMap` (orden) | `map` que deja `Mono<Mono<T>>` |
| Valor ausente → error | `switchIfEmpty(Mono.error(() -> ...))` | `switchIfEmpty(Mono.error(new ...))` (crea la excepción siempre) |
| Valor por defecto costoso | `switchIfEmpty(Mono.defer(...))` | `defaultIfEmpty(calcular())` |
| Combinar independientes | `Mono.zip(a, b)` | `flatMap` anidados en serie |
| Mapeo que puede lanzar al inicio del flujo | `Mono.fromSupplier(...)` | llamarlo fuera del flujo |
| Efectos secundarios (log, métricas) | `doOnNext`, `doOnError` | lógica de negocio dentro de `doOn*` |
| Traducir errores | `onErrorMap` | `onErrorResume` que oculta errores |
| Resultado vacío tras operación | `then()` / `thenReturn(x)` | `map(x -> null)` |
| Código bloqueante | `fromCallable(...).subscribeOn(Schedulers.boundedElastic())` | llamarlo directo en el event loop |

## 9. Pruebas

Solo cuando la fase de pruebas esté aprobada (el usuario puede pedir implementar sin pruebas).

Ubicación: `src/test/java/com/demo_prueba/...` replicando exactamente el paquete de la clase probada, incluida la subcarpeta de contexto (p. ej. `infrastructure/entrypoints/rest/producto/ProductoHandlerTest.java`). AssertJ, Mockito y reactor-test llegan por los starters de test de Spring Boot.

### Modelo

- `@ParameterizedTest` con `@NullAndEmptySource` + `@ValueSource` para valores inválidos; verificar tipo, mensaje y `code`.

### Caso de uso (Mockito + StepVerifier)

```java
@ExtendWith(MockitoExtension.class)
class CrearLibroUseCaseTest {

    @Mock LibroGateway libroGateway;
    @InjectMocks CrearLibroUseCase useCase;

    @Test
    void debeGuardarLibro() {
        var libro = new Libro(null, "Rayuela", "Cortázar");
        var guardado = new Libro("1", "Rayuela", "Cortázar");
        when(libroGateway.guardar(libro)).thenReturn(Mono.just(guardado));

        StepVerifier.create(useCase.ejecutar(libro))
                .expectNext(guardado)
                .verifyComplete();
    }
}
```

### Entrypoint (WebTestClient)

- `WebTestClient.bindToRouterFunction(router.libroRoutes(handler)).handlerStrategies(...)` con el caso de uso mockeado y el mapper real (`new LibroRestMapperImpl()`).
- Verifica 201 + `Location` + body, y los errores 400 a través de `GlobalErrorHandler`.

### Mappers MapStruct

- Unitaria sin Spring con la implementación generada (`new LibroDocumentMapperImpl()`).
- Verifica ida y vuelta y los campos ignorados. En pruebas de adapter/handler usa el mapper real, no un mock.

### Driven adapter

- Unitaria con el repositorio mockeado y el mapper real; verificar que se llama `insert` y no `save`.
- Integración con Mongo (Testcontainers) solo si el usuario la aprueba: añade dependencias.

### `DemoPruebaApplicationTests`

- Es `@SpringBootTest` y levanta los repositorios Mongo: requiere Mongo disponible o una estrategia aprobada (Testcontainers, perfil de test).

### Reglas

- Cubre camino feliz, vacío y error en cada flujo.
- Nunca `block()` en pruebas; usa `StepVerifier`.
- Tiempo/retries: `StepVerifier.withVirtualTime`.

## 10. Entorno y verificación

- El `JAVA_HOME` del sistema puede apuntar a JDK 8; Gradle exige 17+. Ejecutar siempre:

```bash
cd /c/Sura/demo_prueba/demo_prueba
JAVA_HOME="/c/Program Files/Java/jdk-17" ./gradlew compileJava --console=plain   # compilar (genera MapStruct)
JAVA_HOME="/c/Program Files/Java/jdk-17" ./gradlew test --console=plain          # pruebas
JAVA_HOME="/c/Program Files/Java/jdk-17" ./gradlew bootRun                       # levantar (puerto 8080)
```

- Tras mover clases de paquete, usar `clean compileJava` para descartar implementaciones MapStruct generadas obsoletas.
- Prueba manual del endpoint: requiere Mongo en `localhost:27017` (o `MONGODB_URI`). **Una petición exitosa escribe datos reales en `bd_demo`**: avisar al usuario e informar los ids creados. Detener la aplicación al terminar.

```bash
curl --location 'http://localhost:8080/api/v1/productos' \
--header 'Content-Type: application/json' \
--data '{"nombre": "Camiseta", "descripcion": "Talla M"}'
```

## 11. Checklist antes de entregar

- [ ] La fase ejecutada está aprobada y no se adelantó otra.
- [ ] Clases en el paquete que indica `CLAUDE.md` y en la subcarpeta `<contexto>` dentro de `rest` y `mongo`.
- [ ] `domain` no importa Spring, Mongo, Jackson, Lombok ni MapStruct.
- [ ] Gateway solo con operaciones usadas por casos de uso aprobados.
- [ ] Creación con `insert`, no `save`.
- [ ] Mappers MapStruct (`componentModel = "spring"`, `unmappedTargetPolicy = ERROR`) en infraestructura, sin lógica de negocio.
- [ ] `@Builder` solo en documentos y DTOs de infraestructura.
- [ ] Errores vía `GlobalErrorHandler` existente; códigos HTTP y `code` acordados.
- [ ] Configuración en `application.yml` con prefijo `spring.mongodb.*`.
- [ ] Ningún `block()`, `subscribe()`, `Thread.sleep()`, `System.out` en código productivo.
- [ ] Inyección por constructor.
- [ ] Compilación (y pruebas, si la fase las incluye) ejecutadas con JDK 17 y resultado reportado tal cual.
