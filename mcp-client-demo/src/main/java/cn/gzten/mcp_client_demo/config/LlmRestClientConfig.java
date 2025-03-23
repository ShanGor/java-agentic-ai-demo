package cn.gzten.mcp_client_demo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import reactor.netty.http.client.HttpClient;

/**
 * We found that the spring ai client cannot config timeout, so we use RestClient to config it.
 */
@Configuration
@Slf4j
public class LlmRestClientConfig {
    @Bean
    RestClient llmRestClient(CustomAiMcp ai) {
        var responseTimeout = ai.getAi().getResponseTimeout();
        log.info("llmRestClient responseTimeout: {} seconds", responseTimeout.getSeconds());
        HttpClient client = HttpClient.create()
                .responseTimeout(responseTimeout);
        ClientHttpRequestFactory factory = new ReactorClientHttpRequestFactory(client);

        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}
