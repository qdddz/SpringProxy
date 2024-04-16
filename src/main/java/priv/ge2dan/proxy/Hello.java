package priv.ge2dan.proxy;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class Hello {
    private static final Logger log = LoggerFactory.getLogger(Hello.class);

    public static ExchangeFilterFunction errorHandler() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (clientResponse.statusCode().isError()) {
                ClientResponse newResponse = clientResponse.mutate().statusCode(HttpStatus.OK).build();
                newResponse.body((clientHttpResponse, context) -> clientHttpResponse.getBody());
                return Mono.just(newResponse);

            }
            return Mono.just(clientResponse);
        });
    }

    @PostMapping("/v1/test")
    public Mono<?> test(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        return getObjectMono(request);
    }

    private static Mono<Object> getObjectMono(ServerHttpRequest request) {
        return WebClient.builder().baseUrl("http://localhost:5001").filter(errorHandler()).build()
                .post().uri("/test")
                .headers(httpHeaders -> httpHeaders.addAll(request.getHeaders()))
                .body(BodyInserters.fromDataBuffers(request.getBody())).retrieve()
                .bodyToMono(Object.class)
                .onErrorResume(throwable -> Mono.just(new DMM("KP.123", "invoke error")));
    }

    @Data
    @AllArgsConstructor
    private static class DMM {
        private String code;
        private String message;
    }
}
