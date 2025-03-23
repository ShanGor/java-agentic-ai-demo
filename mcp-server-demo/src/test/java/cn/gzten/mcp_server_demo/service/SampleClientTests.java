package cn.gzten.mcp_server_demo.service;

import java.util.Map;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author Christian Tzolov
 */

class SampleClientTests {

    @Test
    public void testList() {
        var transport = new WebFluxSseClientTransport(WebClient.builder().baseUrl("http://localhost:10080"));

        var client = McpClient.sync(transport).build();

        client.initialize();

        client.ping();

        // List and demonstrate tools
        ListToolsResult toolsList = client.listTools();
        System.out.println("Available Tools = " + toolsList);
        for (var tool : toolsList.tools()) {
            System.out.println("\t" + tool.name());
        }


        CallToolResult callToolResult = client.callTool(new CallToolRequest("tellStory",
                Map.of("author", "Green")));
        System.out.println("Tool result: \n\t" + callToolResult.content());

        client.closeGracefully();

    }

}
