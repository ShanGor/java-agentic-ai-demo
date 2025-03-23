package cn.gzten.mcp_client_demo.config;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbackProvider;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Configuration
@Slf4j
public class McpClientConfig {
    @Bean
    public ToolCallbackProvider tools(CustomAiMcp mcp) {
        HttpClient httpClient = HttpClient.create().doOnError((req, err) -> {
            log.error("HttpClient error: {}", err.getMessage());
            }, (resp, err) -> {
            log.error("HttpClient Response error: {}", err.getMessage());
        }).responseTimeout(Duration.ofMillis(30000));

        var connector = new ReactorClientHttpConnector(httpClient);

        List<McpAsyncClient> mcpClients = new ArrayList<>();

        mcp.getMcp().getRemoteServers().forEach(server -> {
            var webclient = WebClient.builder().clientConnector(connector)
                    .baseUrl(server.getUrl());
            var transport = new WebFluxSseClientTransport(webclient);
            McpAsyncClient client = McpClient.async(transport).build();
            mcpClients.add(client);
        });

        return new AsyncMcpToolCallbackProvider(mcpClients);
    }

}
