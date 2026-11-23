# Proyecto

Demo Prueba

# Stack Tecnológico

- Java 17
- Spring Boot
- Spring WebFlux

# Arquitectura

Arquitectura Hexagonal.

La arquitectura definida en este documento es obligatoria para todo el proyecto.

No se debe proponer ni implementar una estructura diferente sin aprobación explícita.

---

# Estructura Base del Proyecto

src/main/java/com/demo_prueba

├── application
│   └── config
│
├── domain
│   ├── model
│   ├── usecase
│   └── exception
│
└── infrastructure
    ├── entrypoints
    │   └── rest
    │       └── <contexto>
    │
    └── drivenadapters
        └── mongo
            └── <contexto>

src/main/resources
└── application.yml

---

# Organización del Dominio

El dominio se organiza por contextos funcionales.

Los contextos NO deben crearse hasta que exista un requerimiento funcional aprobado.

## Estructura obligatoria de un contexto

Cada contexto funcional es una carpeta dentro de `domain/model` y sigue exactamente este patrón:

domain
└── model
    └── libro
        ├── Libro.java
        └── gateway
            └── LibroGateway.java

Reglas:

- La carpeta del contexto se nombra en minúscula y singular: `domain/model/<contexto>`.
- La entidad del contexto va directamente en la raíz de su carpeta: `domain/model/<contexto>/<Entidad>.java`.
- Los objetos de valor y enumeraciones del contexto también van en la raíz de su carpeta.
- El gateway del contexto va en la subcarpeta `gateway`: `domain/model/<contexto>/gateway/<Entidad>Gateway.java`.
- Cada contexto tiene su propia carpeta `gateway`. No existe `domain/gateway` ni ningún gateway global.
- El gateway es una interfaz (contrato); su implementación vive en `infrastructure/drivenadapters`.
- `libro` es únicamente un ejemplo del patrón; no debe crearse sin aprobación.

Ejemplo de estructura futura con varios contextos:

domain

├── model
│
│   ├── producto
│   │   ├── Producto.java
│   │   └── gateway
│   │       └── ProductoGateway.java
│   │
│   ├── cliente
│   │   ├── Cliente.java
│   │   └── gateway
│   │       └── ClienteGateway.java
│   │
│   └── personalizacion
│       ├── Personalizacion.java
│       └── gateway
│           └── PersonalizacionGateway.java
│
├── usecase
│
└── exception

Los nombres anteriores son únicamente ejemplos.

No crear contextos funcionales sin aprobación.

---

# Responsabilidades

## application/config

Responsable de:

- Configuración de Spring.
- Beans.
- Configuraciones transversales.
- Properties.
- Configuración de WebClient.
- Configuración de seguridad cuando aplique.

Reglas:

- Los casos de uso se registran como beans en `application/config/UseCaseConfig.java` (clase compartida por todos los contextos).
- La configuración de la aplicación se define en `src/main/resources/application.yml`. No usar `application.properties`.
- MongoDB (Spring Boot 4) se configura con el prefijo `spring.mongodb.*`. Base de datos: `bd_demo`. URI mediante la variable de entorno `MONGODB_URI`.

---

## domain/model

Responsable de:

- Entidades.
- Objetos de valor.
- Enumeraciones.
- Modelos del dominio.

No incluir:

- Spring.
- Frameworks.
- Clases de infraestructura.
- DTOs.
- Configuración técnica.

---

## domain/model/<contexto>/gateway

Responsable de:

- Puertos de salida.
- Contratos de persistencia.
- Contratos de integraciones externas.

Ejemplo:

domain/model/producto/gateway/ProductoGateway.java

Reglas:

- Cada contexto mantiene sus propios gateways.
- No existe un paquete global llamado gateway.
- Los gateways representan contratos, nunca implementaciones.

---

## domain/usecase

Responsable de:

- Casos de uso.
- Reglas de negocio.
- Orquestación del dominio.

Los casos de uso pueden depender únicamente de:

- domain.model
- domain.model.<contexto>.gateway
- domain.exception

Los casos de uso NO pueden depender de:

- infrastructure
- Spring Data
- WebClient
- Bases de datos
- Frameworks externos

---

## domain/exception

Responsable de:

- Excepciones de negocio.
- Excepciones del dominio.

---

## infrastructure/entrypoints

Responsable de:

- Controllers.
- Handlers.
- Routers.
- DTOs de entrada.
- DTOs de salida.

Reglas:

- No implementar lógica de negocio.
- No acceder directamente a repositorios.
- Utilizar exclusivamente casos de uso.
- Todo lo específico de un contexto va en su subcarpeta: `infrastructure/entrypoints/rest/<contexto>/`.
- Los componentes transversales (por ejemplo `GlobalErrorHandler`) van en la raíz de `infrastructure/entrypoints/rest`.

Ejemplo:

infrastructure/entrypoints/rest
├── GlobalErrorHandler.java
└── producto
    ├── ProductoRouter.java
    ├── ProductoHandler.java
    ├── CrearProductoRequest.java
    ├── ProductoResponse.java
    └── ProductoRestMapper.java

---

## infrastructure/drivenadapters

Responsable de:

- Implementaciones de gateways.
- Integraciones externas.
- Persistencia.
- Clientes de otros sistemas.

Reglas:

- Deben implementar contratos definidos en:
  domain/model/<contexto>/gateway
- No deben contener reglas de negocio.
- Se organizan por tecnología y luego por contexto: `infrastructure/drivenadapters/<tecnologia>/<contexto>/`.
- Tecnología aprobada: `mongo`. Otras tecnologías (WebClient, Kafka, etc.) requieren aprobación.

Ejemplo:

infrastructure/drivenadapters/mongo
└── producto
    ├── ProductoDocument.java
    ├── ProductoMongoRepository.java
    ├── ProductoDocumentMapper.java
    └── ProductoMongoAdapter.java

---

# Programación Reactiva

Todo el proyecto debe seguir principios reactivos.

Uso obligatorio de:

- Mono
- Flux

Prohibido:

- block()
- subscribe()
- Future.get()
- CompletableFuture.join()
- Thread.sleep()

Toda integración nueva debe respetar el paradigma reactivo.

---

# Reglas de Dependencia

DEPENDENCIAS PERMITIDAS

entrypoints → usecase

usecase → model

usecase → gateway

drivenadapters → gateway

drivenadapters → model

entrypoints → model

config → usecase

config → gateway

---

DEPENDENCIAS PROHIBIDAS

model → infrastructure

model → spring

model → drivenadapters

usecase → drivenadapters

usecase → bases de datos

entrypoints → drivenadapters

entrypoints → persistencia

drivenadapters → usecase

---

# Restricciones

- No crear paquetes adicionales sin aprobación.
- No modificar la arquitectura sin aprobación explícita.
- No crear clases de negocio sin aprobación.
- No crear contextos funcionales sin aprobación.
- No crear gateways globales.
- No avanzar automáticamente entre fases.
- No generar código cuando el usuario solicite únicamente análisis o diseño.

---

# Flujo de Trabajo

## Fase 1

Diseño de estructura.

## Fase 2

Creación de carpetas.

## Fase 3

Diseño de artefactos.

## Fase 4

Creación de modelos.

## Fase 5

Creación de gateways.

## Fase 6

Creación de casos de uso.

## Fase 7

Creación de driven adapters.

## Fase 8

Creación de entrypoints.

## Fase 9

Pruebas técnicas.

## Fase 10

Validación arquitectónica.

---

# Approval Gates

Al finalizar cada fase el agente debe:

1. Mostrar el resultado.
2. Explicar las decisiones tomadas.
3. Indicar riesgos o consideraciones.
4. Esperar aprobación explícita.

Palabras válidas para continuar:

- aprobado
- continuar
- siguiente paso
- generar
- proceed

Si no existe aprobación explícita:

DETENERSE.

No continuar automáticamente.

---

# Regla Principal del Agente

Antes de crear carpetas, clases, interfaces o configuraciones:

1. Leer CLAUDE.md.
2. Verificar la arquitectura definida.
3. Proponer la solución.
4. Esperar aprobación.
5. Ejecutar únicamente la fase aprobada.

Nunca saltar fases.

Nunca asumir requerimientos funcionales.

Nunca inventar contextos funcionales.

Nunca modificar la arquitectura establecida.