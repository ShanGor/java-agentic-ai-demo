package org.springframework.ai.ollama;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.MessageAggregator;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.chat.observation.ChatModelObservationDocumentation;
import org.springframework.ai.chat.observation.DefaultChatModelObservationConvention;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import org.springframework.ai.ollama.management.OllamaModelManager;
import org.springframework.ai.ollama.management.PullModelStrategy;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
public class CustomOllamaChatModel extends OllamaChatModel{

    private static final ChatModelObservationConvention DEFAULT_OBSERVATION_CONVENTION = new DefaultChatModelObservationConvention();

    private final OllamaApi chatApi;

    private final ObservationRegistry observationRegistry;

    private final OllamaModelManager modelManager;

    private final ToolCallingManager toolCallingManager;

    private final ChatModelObservationConvention observationConvention = DEFAULT_OBSERVATION_CONVENTION;

    @Setter
    private Consumer<String> streamObserver = null;

    public CustomOllamaChatModel(OllamaApi ollamaApi, OllamaOptions defaultOptions, ToolCallingManager toolCallingManager, ObservationRegistry observationRegistry, ModelManagementOptions modelManagementOptions) {
        super(ollamaApi, defaultOptions, toolCallingManager, observationRegistry, modelManagementOptions);
        this.chatApi = ollamaApi;
        this.toolCallingManager = toolCallingManager;
        this.observationRegistry = observationRegistry;
        this.modelManager = new OllamaModelManager(this.chatApi, modelManagementOptions);
        initializeModel(defaultOptions.getModel(), modelManagementOptions.pullModelStrategy());
    }

    private void initializeModel(String model, PullModelStrategy pullModelStrategy) {
        if (pullModelStrategy != null && !PullModelStrategy.NEVER.equals(pullModelStrategy)) {
            this.modelManager.pullModel(model, pullModelStrategy);
        }
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        // Before moving any further, build the final request Prompt,
        // merging runtime and default options.
        Prompt requestPrompt = buildRequestPrompt(prompt);
        return this.internalStream(requestPrompt, null);
    }

    private Flux<ChatResponse> internalStream(Prompt prompt, ChatResponse previousChatResponse) {
        return Flux.deferContextual(contextView -> {
            OllamaApi.ChatRequest request = ollamaChatRequest(prompt, true);

            final ChatModelObservationContext observationContext = ChatModelObservationContext.builder()
                    .prompt(prompt)
                    .provider(OllamaApi.PROVIDER_NAME)
                    .requestOptions(prompt.getOptions())
                    .build();

            Observation observation = ChatModelObservationDocumentation.CHAT_MODEL_OPERATION.observation(
                    this.observationConvention, DEFAULT_OBSERVATION_CONVENTION, () -> observationContext,
                    this.observationRegistry);

            observation.parentObservation(contextView.getOrDefault(ObservationThreadLocalAccessor.KEY, null)).start();

            Flux<OllamaApi.ChatResponse> ollamaResponse = this.chatApi.streamingChat(request);

            Flux<ChatResponse> chatResponse = ollamaResponse.map(chunk -> {
                String content = (chunk.message() != null) ? chunk.message().content() : "";
                if (streamObserver != null && !"".equals(content)) {
                    streamObserver.accept(content);
                }

                List<AssistantMessage.ToolCall> toolCalls = List.of();

                // Added null checks to prevent NPE when accessing tool calls
                if (chunk.message() != null && chunk.message().toolCalls() != null) {
                    toolCalls = chunk.message()
                            .toolCalls()
                            .stream()
                            .map(toolCall -> new AssistantMessage.ToolCall("", "function", toolCall.function().name(),
                                    ModelOptionsUtils.toJsonString(toolCall.function().arguments())))
                            .toList();
                }

                var assistantMessage = new AssistantMessage(content, Map.of(), toolCalls);

                ChatGenerationMetadata generationMetadata = ChatGenerationMetadata.NULL;
                if (chunk.promptEvalCount() != null && chunk.evalCount() != null) {
                    generationMetadata = ChatGenerationMetadata.builder().finishReason(chunk.doneReason()).build();
                }

                var generator = new Generation(assistantMessage, generationMetadata);
                return new ChatResponse(List.of(generator), from(chunk, previousChatResponse));
            });

            // @formatter:off
            Flux<ChatResponse> chatResponseFlux = chatResponse.flatMap(response -> {
                        if (ToolCallingChatOptions.isInternalToolExecutionEnabled(prompt.getOptions()) && response.hasToolCalls()) {
                            var toolExecutionResult = this.toolCallingManager.executeToolCalls(prompt, response);
                            if (toolExecutionResult.returnDirect()) {
                                // Return tool execution result directly to the client.
                                return Flux.just(ChatResponse.builder().from(response)
                                        .generations(ToolExecutionResult.buildGenerations(toolExecutionResult))
                                        .build());
                            } else {
                                // Send the tool execution result back to the model.
                                return this.internalStream(new Prompt(toolExecutionResult.conversationHistory(), prompt.getOptions()),
                                        response);
                            }
                        }
                        else {
                            return Flux.just(response);
                        }
                    })
                    .doOnError(observation::error)
                    .doFinally(s ->
                            observation.stop()
                    )
                    .contextWrite(ctx -> ctx.put(ObservationThreadLocalAccessor.KEY, observation));
            // @formatter:on

            return new MessageAggregator().aggregate(chatResponseFlux, observationContext::setResponse);
        });
    }
}
