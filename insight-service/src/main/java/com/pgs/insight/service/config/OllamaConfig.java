package com.pgs.insight.service.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration of the chat model used to generate the energy insights.
 * <p>
 * Declares the {@link ChatClient} bean with the default system prompt that
 * instructs the model to act as an expert energy efficiency advisor.
 */
@Configuration
public class OllamaConfig {

    /**
     * Builds the {@link ChatClient} used to request insights to the Ollama chat
     * model, configuring the default system prompt.
     *
     * @param builder the builder provided by Spring AI
     * @return the configured chat client
     */
    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("""
                        You are an expert energy efficiency advisor. \
                        Provide concise and practical advice to users on how to reduce \
                        their energy consumption based on their usage patterns.\
                        """)
                .build();
    }
}