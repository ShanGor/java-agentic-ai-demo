package cn.gzten.mcp_client_demo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestClient;

import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * We found that the spring ai client cannot config timeout, so we use RestClient to config it.
 */
@Configuration
@Slf4j
public class LlmRestClientConfig {
    @Bean
    RestClient.Builder llmRestClient(CustomAiMcp ai) {
        var responseTimeout = ai.getAi().getResponseTimeout();
        log.info("llmRestClient responseTimeout: {} seconds", responseTimeout.getSeconds());
        HttpClient client = HttpClient.create()
                .responseTimeout(responseTimeout);
        ClientHttpRequestFactory factory = new ReactorClientHttpRequestFactory(client);

        return RestClient.builder()
                .requestFactory(factory);
    }

    @Bean
    WebClient.Builder llmWebClient(CustomAiMcp ai) {
        var responseTimeout = ai.getAi().getResponseTimeout();
        log.info("llmWebClient responseTimeout: {} seconds", responseTimeout.getSeconds());
        HttpClient client = HttpClient.create()
                .responseTimeout(responseTimeout);

        return WebClient.builder().clientConnector(new ReactorClientHttpConnector(client));
    }
}
