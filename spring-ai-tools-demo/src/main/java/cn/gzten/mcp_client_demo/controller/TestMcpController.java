package cn.gzten.mcp_client_demo.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.*;

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
