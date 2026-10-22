package com.pgs.insight.service.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.pgs.insight.service.client.UsageClient;
import com.pgs.insight.service.dto.DeviceDto;
import com.pgs.insight.service.dto.UsageDto;

/**
 * End-to-end test that boots the whole application and exercises the HTTP API
 * through the real controller, service, aspects and exception handler.
 *
 * <p>
 * Only the two external collaborators are mocked: the {@link UsageClient}
 * (the Usage Service) and the Ollama chat model. No Usage Service and no
 * Ollama instance are required.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@DisplayName("Insight API end-to-end")
class InsightApiIntegrationTest {

	private static final String BASE_URL = "/api/v1/insight";

	@Autowired
	private TestRestTemplate restTemplate;

	@MockitoBean
	private UsageClient usageClient;

	@MockitoBean
	private org.springframework.ai.ollama.OllamaChatModel ollamaChatModel;

	@BeforeEach
	void setUp() {
		UsageDto usage = UsageDto.builder()
				.userId(42L)
				.devices(
					List.of(
						DeviceDto.builder().id(1L).name("Bulb").type("LIGHT").location("Salon")
								.energyConsumed(3.5).build(),
						DeviceDto.builder().id(2L).name("Thermostat").type("CLIMATE").location("Cocina")
								.energyConsumed(1.25).build())
					)
				.build();

		when(usageClient.getXDaysUsageForUser(anyLong(), anyInt())).thenReturn(usage);
		when(ollamaChatModel.call(any(Prompt.class)))
				.thenReturn(new ChatResponse(List.of(
						new Generation(new AssistantMessage("Shift heavy loads to off-peak hours.")))));
	}

	@Nested
	@DisplayName("GET /api/v1/insight/saving-tips/{userId}")
	class SavingTips {

		@Test
		@DisplayName("returns 200 with the generated tips and the aggregated usage")
		void returns200WithGeneratedTips() {
			ResponseEntity<String> response =
					restTemplate.getForEntity(BASE_URL + "/saving-tips/42", String.class);

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(response.getBody())
					.contains("Shift heavy loads to off-peak hours.")
					.contains("\"userId\":42")
					.contains("\"energyUsage\":4.75");
		}
	}

	@Nested
	@DisplayName("GET /api/v1/insight/overview/{userId}")
	class Overview {

		@Test
		@DisplayName("returns 200 with the generated overview and the aggregated usage")
		void returns200WithGeneratedOverview() {
			ResponseEntity<String> response =
					restTemplate.getForEntity(BASE_URL + "/overview/42", String.class);

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(response.getBody())
					.contains("Shift heavy loads to off-peak hours.")
					.contains("\"userId\":42")
					.contains("\"energyUsage\":4.75");
		}
	}

	@Nested
	@DisplayName("invalid requests")
	class InvalidRequests {

		@Test
		@DisplayName("returns 400 with the error contract when the userId is not a number")
		void returns400WithErrorContract() {
			ResponseEntity<String> response =
					restTemplate.getForEntity(BASE_URL + "/saving-tips/abc", String.class);

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
			assertThat(response.getBody())
					.contains("\"status\":400")
					.contains("\"error\":\"Bad Request\"")
					.contains("Parameter 'userId' must be of type 'Long'");
		}
	}
}
