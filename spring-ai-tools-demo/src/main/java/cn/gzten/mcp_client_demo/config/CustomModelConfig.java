package cn.gzten.mcp_client_demo.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.autoconfigure.ollama.OllamaChatProperties;
import org.springframework.ai.autoconfigure.ollama.OllamaConnectionDetails;
import org.springframework.ai.autoconfigure.ollama.OllamaInitializationProperties;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.CustomOllamaApi;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import org.springframework.ai.ollama.management.PullModelStrategy;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.function.Consumer;

@Configuration
public class CustomModelConfig {
    private final OllamaOptions defaultOptions;
    private final ToolCallingManager toolCallingManager;
    private final ObservationRegistry observationRegistry;
    private final ModelManagementOptions modelManagementOptions;
    private final OllamaConnectionDetails connectionDetails;
    private final ObjectProvider<RestClient.Builder> restClientBuilderProvider;
    private final ObjectProvider<WebClient.Builder> webClientBuilderProvider;

    /**
     * Copy the logic from org.springframework.ai.autoconfigure.ollama.OllamaAutoConfiguration
     *
     */
    public CustomModelConfig(OllamaChatProperties properties,
                             OllamaInitializationProperties initProperties, ToolCallingManager toolCallingManager,
                             ObjectProvider<ObservationRegistry> observationRegistry,
                             OllamaConnectionDetails connectionDetails,
                             ObjectProvider<RestClient.Builder> restClientBuilderProvider,
                             ObjectProvider<WebClient.Builder> webClientBuilderProvider) {
        var chatModelPullStrategy = initProperties.getChat().isInclude() ? initProperties.getPullModelStrategy()
                : PullModelStrategy.NEVER;
        this.defaultOptions = properties.getOptions();
        this.toolCallingManager = toolCallingManager;
        this.observationRegistry = observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP);
        this.modelManagementOptions = new ModelManagementOptions(chatModelPullStrategy, initProperties.getChat().getAdditionalModels(),
                initProperties.getTimeout(), initProperties.getMaxRetries());
        this.connectionDetails = connectionDetails;
        this.restClientBuilderProvider = restClientBuilderProvider;
        this.webClientBuilderProvider = webClientBuilderProvider;
    }

    public OllamaChatModel customOllamaChatModel(Consumer<OllamaApi.ChatResponse> streamObserver) {
        var ollamaApi = new CustomOllamaApi(connectionDetails.getBaseUrl(),
                restClientBuilderProvider.getIfAvailable(RestClient::builder),
                webClientBuilderProvider.getIfAvailable(WebClient::builder));
        ollamaApi.setStreamObserver(streamObserver);
        return new OllamaChatModel(ollamaApi, defaultOptions, toolCallingManager, observationRegistry, modelManagementOptions);
    }
}
