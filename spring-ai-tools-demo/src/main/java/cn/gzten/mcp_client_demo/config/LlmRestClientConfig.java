package cn.gzten.mcp_client_demo.config;

import io.netty.handler.codec.http.HttpHeaders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestClient;

import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

/**
 * We found that the spring ai client cannot config timeout, so we use RestClient to config it.
 */
@Configuration
@Slf4j
public class LlmRestClientConfig {

    private final Function<? super HttpHeaders, Mono<? extends HttpHeaders>> authKeyInterceptor = headers -> Mono.just(headers.set("Authorization", authKey()));

    /**
     * in big companies, they might use a short-living API key to access LLM services, like changes every minute. We need to be able to refresh it.
     * This `authKey()` method is for you to implement your own logic to get the authKey.
     */
    private String authKey() {
        var dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        var key = "sk-" + dtf.format(LocalDateTime.now());
        log.info("Pretending to be using authKey: {}", key);
        return key;
    }

    @Bean
    RestClient.Builder llmRestClient(CustomAiMcp ai) {
        var responseTimeout = ai.getAi().getResponseTimeout();
        log.info("llmRestClient responseTimeout: {} seconds", responseTimeout.getSeconds());
        HttpClient client = HttpClient.create().headersWhen(authKeyInterceptor)
                .responseTimeout(responseTimeout);
        ClientHttpRequestFactory factory = new ReactorClientHttpRequestFactory(client);

        return RestClient.builder()
                .requestFactory(factory);
    }

    @Bean
    WebClient.Builder llmWebClient(CustomAiMcp ai) {
        var responseTimeout = ai.getAi().getResponseTimeout();
        log.info("llmWebClient responseTimeout: {} seconds", responseTimeout.getSeconds());
        HttpClient client = HttpClient.create().headersWhen(authKeyInterceptor)
                .responseTimeout(responseTimeout);

        return WebClient.builder().clientConnector(new ReactorClientHttpConnector(client));
    }
}
