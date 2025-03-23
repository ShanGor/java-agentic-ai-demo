package cn.gzten.mcp_client_demo.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class TestMcpController {
    private final ChatClient.Builder chatClientBuilder;
    private final ToolCallbackProvider tools;
    @GetMapping("/test")
    public void test() {
        Thread.ofVirtual().start(() -> {var chatClient = chatClientBuilder
                .defaultTools(tools)
                .defaultAdvisors(new MessageChatMemoryAdvisor(new InMemoryChatMemory()))
                .build();

            var prompt = """
                The below BigTask might need to split into small sub-tasks, and sometimes you need to call a tool to accomplish the sub-tasks. Please do one task at a time, and I will loop you until all sub-tasks completed!
                BigTask:
                Tell me a story around 100 words with author Green, then save the whole story as a text file
                """;

            System.out.println("\nI am your AI assistant.\n");
            System.out.print("\nUSER: " + prompt);
            var output = chatClient.prompt(prompt)
                    .call()
                    .content();
            System.out.println("\nASSISTANT: " + output);
        });

    }
}
