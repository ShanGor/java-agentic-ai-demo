package cn.gzten.mcp_client_demo.controller;

import cn.gzten.mcp_client_demo.util.IntegrationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.Media;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

@RestController
@Slf4j
public class TestMcpController {
    private final ChatModel chatModel;
    private final ChatClient.Builder chatClientBuilder;
    private final ToolCallbackProvider tools;

    private final ToolCallingManager toolCallingManager;

    public TestMcpController(ChatModel chatModel,
                             ChatClient.Builder chatClientBuilder,
                             ToolCallbackProvider tools) {
        this.chatModel = chatModel;
        this.chatClientBuilder = chatClientBuilder;
        this.tools = tools;
        toolCallingManager = ToolCallingManager.builder().build();
    }

    @GetMapping("/test-image")
    public void testMultiModel() throws IOException {
        var imageUrl = "data:image/jpeg;base64,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";

        var image = IntegrationUtils.convertDataUrlToMedia(imageUrl);
        Thread.ofVirtual().start(() -> {

            var result = chatClientBuilder.build().prompt()
                    .user(u -> u.text("Explain what you see in this picture").media(image))
                    .call().content();
            System.out.println("Assistant: " + result);
        });

    }

    @GetMapping("/test")
    public Flux<ServerSentEvent<String>> withoutInternalExecution() {
        ChatOptions chatOptions = ToolCallingChatOptions.builder()
                .toolCallbacks(tools.getToolCallbacks())
                .internalToolExecutionEnabled(false)
                .build();

        var myPrompt = """
                The below BigTask might need to split into small sub-tasks, and sometimes you need to call a tool to accomplish the sub-tasks. Please do one task at a time, and I will loop you until all sub-tasks completed!
                BigTask:
                Tell me a story about 500 words with author Green, then save the whole story as a text file
                """;

        return Flux.create(sink -> Thread.ofVirtual().start(() -> {
            var prompt = new Prompt(myPrompt, chatOptions);
            var resp = chatModel.call(prompt);
            while(resp != null && resp.hasToolCalls()) {
                resp.getResult().getOutput().getToolCalls().forEach(toolCall -> {
                    sink.next(ServerSentEvent.builder("%s: %s".formatted(toolCall.name(), toolCall.arguments())).event("tool-call").build());
                });

                ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, resp);
                if (toolExecutionResult.conversationHistory().getLast() instanceof ToolResponseMessage message) {
                    message.getResponses().forEach(response -> {
                        sink.next(ServerSentEvent.builder(response.responseData()).event("tool-result").build());
                    });
                }

                prompt = new Prompt(toolExecutionResult.conversationHistory(), chatOptions);

                resp = chatModel.call(prompt);
            }

            if (resp != null) {
                sink.next(ServerSentEvent.builder(resp.getResult().getOutput().getText()).event("result").build());
            }
            sink.complete();
        }));
    }

    @GetMapping("/test-default-loop")
    public Flux<ServerSentEvent<String>> withAgentInternalExecution() {
        ChatClient chatClient = chatClientBuilder
                .defaultTools(tools)
                .defaultAdvisors(new MessageChatMemoryAdvisor(new InMemoryChatMemory()))
                .build();

        var myPrompt = """
                The below BigTask might need to split into small sub-tasks, and sometimes you need to call a tool to accomplish the sub-tasks. Please do one task at a time, and I will loop you until all sub-tasks completed!
                BigTask:
                Tell me a story with author Green, then save the whole story as a text file
                """;

        return Flux.defer(() -> chatClient.prompt(myPrompt).stream().content()).map(content -> ServerSentEvent.builder(content).event("result").build());
    }

}
