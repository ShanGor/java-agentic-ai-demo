package cn.gzten.mcp_client_demo.config;

import cn.gzten.mcp_client_demo.service.FairyTaleService;
import cn.gzten.mcp_client_demo.service.FilingService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ToolsConfig {
    private final FairyTaleService fairyTaleService;
    private final FilingService filingService;

    @Bean
    public ToolCallbackProvider tools() {
        return MethodToolCallbackProvider.builder()
                .toolObjects(fairyTaleService, filingService)
                .build();
    }
}
