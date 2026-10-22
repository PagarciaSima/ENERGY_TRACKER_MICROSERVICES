package com.pgs.insight.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;

import com.pgs.insight.service.client.UsageClient;
import com.pgs.insight.service.dto.DeviceDto;
import com.pgs.insight.service.dto.InsightDto;
import com.pgs.insight.service.dto.UsageDto;

/**
 * Unit tests for {@link InsightService}.
 *
 * <p>
 * Both collaborators are mocked: the {@link UsageClient} stands for the
 * Usage Service and the {@link OllamaChatModel} stands for the local LLM, so
 * neither an HTTP service nor an Ollama instance is required.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InsightService")
class InsightServiceTest {

	@Mock
	private UsageClient usageClient;

	@Mock
	private OllamaChatModel ollamaChatModel;

	@Captor
	private ArgumentCaptor<Prompt> promptCaptor;

	private InsightService insightService;

	@BeforeEach
	void setUp() {
		insightService = new InsightService(usageClient, ollamaChatModel);
	}

	/* ------------------------------------------------------------------ */
	/*  Test data helpers                                                 */
	/* ------------------------------------------------------------------ */

	private static UsageDto usageWithDevices() {
		return UsageDto.builder()
				.userId(42L)
				.devices(List.of(
						DeviceDto.builder().id(1L).name("Bulb").type("LIGHT").location("Salon")
								.energyConsumed(3.5).build(),
						DeviceDto.builder().id(2L).name("Thermostat").type("CLIMATE").location("Cocina")
								.energyConsumed(1.25).build()))
				.build();
	}

	private static ChatResponse chatResponse(String text) {
		return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
	}

	/* ------------------------------------------------------------------ */
	/*  getSavingsTips                                                    */
	/* ------------------------------------------------------------------ */

	@Nested
	@DisplayName("getSavingsTips")
	class GetSavingsTips {

		@Test
		@DisplayName("returns the model output and the aggregated energy usage")
		void returnsModelOutputAndAggregatedUsage() {
			when(usageClient.getXDaysUsageForUser(42L, 3)).thenReturn(usageWithDevices());
			when(ollamaChatModel.call(any(Prompt.class)))
					.thenReturn(chatResponse("Shift heavy loads to off-peak hours."));

			InsightDto insight = insightService.getSavingsTips(42L);

			assertThat(insight).isNotNull();
			assertThat(insight.userId()).isEqualTo(42L);
			assertThat(insight.tips()).isEqualTo("Shift heavy loads to off-peak hours.");
			assertThat(insight.energyUsage()).isEqualTo(4.75);
		}

		@Test
		@DisplayName("requests the usage of the last 3 days for the given user")
		void requestsLastThreeDaysForTheGivenUser() {
			when(usageClient.getXDaysUsageForUser(42L, 3)).thenReturn(usageWithDevices());
			when(ollamaChatModel.call(any(Prompt.class))).thenReturn(chatResponse("tips"));

			insightService.getSavingsTips(42L);

			verify(usageClient).getXDaysUsageForUser(42L, 3);
		}

		@Test
		@DisplayName("sends a prompt containing the total energy consumed")
		void sendsPromptContainingTotalUsage() {
			when(usageClient.getXDaysUsageForUser(42L, 3)).thenReturn(usageWithDevices());
			when(ollamaChatModel.call(any(Prompt.class))).thenReturn(chatResponse("tips"));

			insightService.getSavingsTips(42L);

			verify(ollamaChatModel).call(promptCaptor.capture());
			assertThat(promptCaptor.getValue().getContents())
					.contains("4.75")
					.contains("reduce my energy consumption");
		}

		@Test
		@DisplayName("returns zero energy usage when the user has no devices")
		void returnsZeroUsageWhenNoDevices() {
			when(usageClient.getXDaysUsageForUser(42L, 3))
					.thenReturn(UsageDto.builder().userId(42L).devices(List.of()).build());
			when(ollamaChatModel.call(any(Prompt.class))).thenReturn(chatResponse("No devices found."));

			InsightDto insight = insightService.getSavingsTips(42L);

			assertThat(insight.energyUsage()).isZero();
			assertThat(insight.tips()).isEqualTo("No devices found.");
		}

		@Test
		@DisplayName("propagates the failure of the usage service")
		void propagatesUsageServiceFailure() {
			when(usageClient.getXDaysUsageForUser(eq(42L), eq(3)))
					.thenThrow(new IllegalStateException("usage service unavailable"));

			assertThatThrownBy(() -> insightService.getSavingsTips(42L))
					.isInstanceOf(IllegalStateException.class)
					.hasMessage("usage service unavailable");
			verify(ollamaChatModel, never()).call(any(Prompt.class));
		}
	}

	/* ------------------------------------------------------------------ */
	/*  getOverview                                                       */
	/* ------------------------------------------------------------------ */

	@Nested
	@DisplayName("getOverview")
	class GetOverview {

		@Test
		@DisplayName("returns the model output and the aggregated energy usage")
		void returnsModelOutputAndAggregatedUsage() {
			when(usageClient.getXDaysUsageForUser(42L, 3)).thenReturn(usageWithDevices());
			when(ollamaChatModel.call(any(Prompt.class)))
					.thenReturn(chatResponse("Your consumption is in line with similar households."));

			InsightDto insight = insightService.getOverview(42L);

			assertThat(insight).isNotNull();
			assertThat(insight.userId()).isEqualTo(42L);
			assertThat(insight.tips()).isEqualTo("Your consumption is in line with similar households.");
			assertThat(insight.energyUsage()).isEqualTo(4.75);
		}

		@Test
		@DisplayName("sends a prompt containing the usage data of the devices")
		void sendsPromptContainingDeviceData() {
			when(usageClient.getXDaysUsageForUser(42L, 3)).thenReturn(usageWithDevices());
			when(ollamaChatModel.call(any(Prompt.class))).thenReturn(chatResponse("overview"));

			insightService.getOverview(42L);

			verify(ollamaChatModel).call(promptCaptor.capture());
			assertThat(promptCaptor.getValue().getContents())
					.contains("Bulb")
					.contains("Thermostat")
					.contains("3.5");
		}

		@Test
		@DisplayName("propagates the failure of the chat model")
		void propagatesChatModelFailure() {
			when(usageClient.getXDaysUsageForUser(42L, 3)).thenReturn(usageWithDevices());
			when(ollamaChatModel.call(any(Prompt.class)))
					.thenThrow(new IllegalStateException("ollama unreachable"));

			assertThatThrownBy(() -> insightService.getOverview(42L))
					.isInstanceOf(IllegalStateException.class)
					.hasMessage("ollama unreachable");
		}
	}
}

