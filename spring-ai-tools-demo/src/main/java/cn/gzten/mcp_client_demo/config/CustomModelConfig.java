package cn.gzten.mcp_client_demo.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.autoconfigure.ollama.OllamaChatProperties;
import org.springframework.ai.autoconfigure.ollama.OllamaInitializationProperties;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.ollama.CustomOllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import org.springframework.ai.ollama.management.PullModelStrategy;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class CustomModelConfig {
    private final OllamaApi ollamaApi;
    private final OllamaOptions defaultOptions;
    private final ToolCallingManager toolCallingManager;
    private final ObservationRegistry observationRegistry;
    private final ModelManagementOptions modelManagementOptions;

    /**
     * Copy the logic from org.springframework.ai.autoconfigure.ollama.OllamaAutoConfiguration
     *
     */
    public CustomModelConfig(OllamaApi ollamaApi, OllamaChatProperties properties,
                             OllamaInitializationProperties initProperties, ToolCallingManager toolCallingManager,
                             ObjectProvider<ObservationRegistry> observationRegistry) {
        var chatModelPullStrategy = initProperties.getChat().isInclude() ? initProperties.getPullModelStrategy()
                : PullModelStrategy.NEVER;
        this.ollamaApi = ollamaApi;
        this.defaultOptions = properties.getOptions();
        this.toolCallingManager = toolCallingManager;
        this.observationRegistry = observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP);
        this.modelManagementOptions = new ModelManagementOptions(chatModelPullStrategy, initProperties.getChat().getAdditionalModels(),
                initProperties.getTimeout(), initProperties.getMaxRetries());
    }

    public CustomOllamaChatModel customOllamaChatModel(Consumer<String> streamObserver) {
        var model = new CustomOllamaChatModel(ollamaApi, defaultOptions, toolCallingManager, observationRegistry, modelManagementOptions);
        model.setStreamObserver(streamObserver);
        return model;
    }
}
