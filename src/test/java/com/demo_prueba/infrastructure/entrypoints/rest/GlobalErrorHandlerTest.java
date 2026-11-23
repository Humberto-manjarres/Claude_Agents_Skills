package com.demo_prueba.infrastructure.entrypoints.rest;

import org.junit.jupiter.api.Test;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalErrorHandlerTest {

    private static final String PATH = "/api/v1/productos";

    private final GlobalErrorHandler handler = new GlobalErrorHandler(ServerCodecConfigurer.create());

    private static MockServerWebExchange exchange() {
        return MockServerWebExchange.from(MockServerHttpRequest.post(PATH));
    }

    @Test
    void debeTraducirDecodingExceptionA400SolicitudInvalida() {
        var exchange = exchange();

        StepVerifier.create(handler.handle(exchange, new DecodingException("json mal formado")))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exchange.getResponse().getHeaders().getContentType())
                .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        StepVerifier.create(exchange.getResponse().getBodyAsString())
                .assertNext(body -> assertThat(body)
                        .contains("\"code\":\"SOLICITUD_INVALIDA\"")
                        .contains("\"instance\":\"" + PATH + "\"")
                        .doesNotContain("json mal formado"))
                .verifyComplete();
    }

    @Test
    void debeDelegarOtrasResponseStatusException() {
        var exchange = exchange();
        var ex = new ResponseStatusException(HttpStatus.NOT_FOUND);

        StepVerifier.create(handler.handle(exchange, ex))
                .expectErrorSatisfies(error -> assertThat(error).isSameAs(ex))
                .verify();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void debePropagarErrorCuandoLaRespuestaYaFueComprometida() {
        var exchange = exchange();
        var ex = new IllegalStateException("respuesta ya enviada");

        StepVerifier.create(exchange.getResponse().setComplete()
                        .then(handler.handle(exchange, ex)))
                .expectErrorSatisfies(error -> assertThat(error).isSameAs(ex))
                .verify();
    }
}
