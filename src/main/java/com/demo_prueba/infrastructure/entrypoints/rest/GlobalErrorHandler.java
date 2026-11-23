package com.demo_prueba.infrastructure.entrypoints.rest;

import com.demo_prueba.domain.exception.BusinessException;
import java.net.URI;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

/**
 * Traduce las excepciones a respuestas ProblemDetail (RFC 9457) con la propiedad adicional "code".
 * Se ejecuta antes que el manejador de errores por defecto de Spring Boot (orden -1).
 * Las ResponseStatusException distintas de errores de lectura del cuerpo (404, 405, 415...)
 * se delegan al manejador por defecto para conservar su código HTTP.
 */
@Component
@Order(-2)
public class GlobalErrorHandler implements WebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorHandler.class);

    private static final String CODE_PROPERTY = "code";
    private static final String CODIGO_SOLICITUD_INVALIDA = "SOLICITUD_INVALIDA";
    private static final String CODIGO_ERROR_INTERNO = "ERROR_INTERNO";

    private final ServerResponse.Context responseContext;

    public GlobalErrorHandler(ServerCodecConfigurer codecConfigurer) {
        List<HttpMessageWriter<?>> writers = codecConfigurer.getWriters();
        this.responseContext = new ServerResponse.Context() {
            @Override
            public List<HttpMessageWriter<?>> messageWriters() {
                return writers;
            }

            @Override
            public List<ViewResolver> viewResolvers() {
                return List.of();
            }
        };
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }
        if (ex instanceof BusinessException businessException) {
            return escribir(exchange, problema(HttpStatus.BAD_REQUEST, "Solicitud inválida",
                    businessException.getMessage(), businessException.getCode()));
        }
        if (ex instanceof ServerWebInputException || ex instanceof DecodingException) {
            return escribir(exchange, problema(HttpStatus.BAD_REQUEST, "Solicitud inválida",
                    "El cuerpo de la solicitud es inválido o está ausente", CODIGO_SOLICITUD_INVALIDA));
        }
        if (ex instanceof ResponseStatusException) {
            return Mono.error(ex);
        }
        log.error("Error no controlado procesando {} {}", exchange.getRequest().getMethod(),
                exchange.getRequest().getPath(), ex);
        return escribir(exchange, problema(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno",
                "Ocurrió un error inesperado", CODIGO_ERROR_INTERNO));
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalle, String codigo) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detalle);
        problemDetail.setTitle(titulo);
        problemDetail.setProperty(CODE_PROPERTY, codigo);
        return problemDetail;
    }

    private Mono<Void> escribir(ServerWebExchange exchange, ProblemDetail problemDetail) {
        problemDetail.setInstance(URI.create(exchange.getRequest().getPath().value()));
        return ServerResponse.status(problemDetail.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyValue(problemDetail)
                .flatMap(response -> response.writeTo(exchange, responseContext));
    }
}
