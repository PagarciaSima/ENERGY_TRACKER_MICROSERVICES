package com.pgs.insight.service.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;

/**
 * Unit tests for {@link OllamaConfig}.
 *
 * <p>
 * The {@link ChatClient.Builder} supplied by Spring AI is mocked so the
 * configuration can be verified in isolation, without a running Ollama
 * instance.
 */
@DisplayName("OllamaConfig")
class OllamaConfigTest {

	private ChatClient.Builder builder;
	private ChatClient chatClient;
	private OllamaConfig config;

	@BeforeEach
	void setUp() {
		builder = mock(ChatClient.Builder.class);
		chatClient = mock(ChatClient.class);
		when(builder.defaultSystem(anyString())).thenReturn(builder);
		when(builder.build()).thenReturn(chatClient);
		config = new OllamaConfig();
	}

	@Test
	@DisplayName("builds a chat client with the energy advisor system prompt")
	void buildsChatClientWithEnergyAdvisorSystemPrompt() {
		ChatClient result = config.chatClient(builder);

		assertThat(result).isSameAs(chatClient);
		verify(builder).build();
	}

	@Test
	@DisplayName("configures a default system prompt mentioning energy efficiency")
	void configuresDefaultSystemPrompt() {
		config.chatClient(builder);

		ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
		verify(builder).defaultSystem(promptCaptor.capture());

		assertThat(promptCaptor.getValue())
				.contains("energy efficiency advisor")
				.contains("reduce their energy consumption");
	}
}
