package com.pgs.insight.service.service;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import com.pgs.insight.service.client.UsageClient;
import com.pgs.insight.service.dto.DeviceDto;
import com.pgs.insight.service.dto.InsightDto;
import com.pgs.insight.service.dto.UsageDto;
import com.pgs.insight.service.exception.DependencyUnavailableException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service that generates energy-consumption insights for a given user by
 * combining data retrieved from the Usage Service with responses produced by
 * an Ollama chat model.
 * <p>
 * Both collaborators are remote, so their HTTP failures are translated into a
 * {@link DependencyUnavailableException} (reported as {@code 503 Service
 * Unavailable}) instead of leaking the HTTP client exception as an unexpected
 * error.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InsightService {

	/** Number of days of usage data used as input for the insights. */
	private static final int LAST_THREE_DAYS_USAGE = 3;

	/** Name of the usage dependency, used in the error messages. */
	private static final String USAGE_SERVICE = "Usage Service";

	/** Name of the chat model dependency, used in the error messages. */
	private static final String OLLAMA = "Ollama";

	private final UsageClient usageClient;
	private final OllamaChatModel ollamaChatModel;

	/**
	 * Builds personalized energy-saving tips for the given user.
	 *
	 * <p>It fetches the last three days of usage data from the Usage Service,
	 * aggregates the total energy consumed and asks the Ollama chat model for
	 * saving recommendations.</p>
	 *
	 * @param userId identifier of the user
	 * @return an {@link InsightDto} containing the generated tips and the total
	 *         energy usage over the analysed period
	 * @throws DependencyUnavailableException if the Usage Service or the chat
	 *                                        model cannot be reached
	 */
	public InsightDto getSavingsTips(final Long userId) {
		final UsageDto usageData = fetchUsage(userId);
		final double totalUsage = calculateTotalUsage(usageData);

		log.info("Calling Ollama for userId {} with total usage {}", userId, totalUsage);

		final String prompt = getSavingPrompt(totalUsage);

		return buildInsight(userId, prompt, totalUsage);
	}

	/**
	 * Fetches the aggregated usage of the last three days for the given user,
	 * translating HTTP failures into a {@link DependencyUnavailableException}.
	 *
	 * @param userId identifier of the user
	 * @return the usage data returned by the Usage Service
	 * @throws DependencyUnavailableException if the Usage Service is not
	 *                                        reachable or answers with an error
	 */
	private UsageDto fetchUsage(final Long userId) {
		try {
			return usageClient.getXDaysUsageForUser(userId, LAST_THREE_DAYS_USAGE);
		} catch (RestClientException e) {
			log.error("Usage Service is unavailable for userId {}: {}", userId, e.getMessage());
			throw new DependencyUnavailableException(USAGE_SERVICE, e.getMessage(), e);
		}
	}

	/**
	 * Builds a general overview of the user's energy usage.
	 *
	 * <p>It fetches the last three days of usage data from the Usage Service,
	 * aggregates the total energy consumed and asks the Ollama chat model for a
	 * concise analysis with actionable insights.</p>
	 *
	 * @param userId identifier of the user
	 * @return an {@link InsightDto} containing the generated overview and the
	 *         total energy usage over the analysed period
	 * @throws DependencyUnavailableException if the Usage Service or the chat
	 *                                        model cannot be reached
	 */
	public InsightDto getOverview(final Long userId) {
		final UsageDto usageData = fetchUsage(userId);
		final double totalUsage = calculateTotalUsage(usageData);

		log.info("Calling Ollama for userId {} with total usage {}", userId, totalUsage);

		final String prompt = getOverviewPrompt(usageData);

		return buildInsight(userId, prompt, totalUsage);
	}

	/**
	 * Sums the energy consumed across all devices in the given usage data.
	 *
	 * @param usageData usage data retrieved from the Usage Service
	 * @return the total energy consumed by all devices
	 */
	private double calculateTotalUsage(final UsageDto usageData) {
		return usageData.devices().stream()
				.mapToDouble(DeviceDto::energyConsumed)
				.sum();
	}

	/**
	 * Sends the given prompt to the Ollama chat model and wraps the result in
	 * an {@link InsightDto}.
	 *
	 * @param userId     identifier of the user
	 * @param prompt     prompt to send to the model
	 * @param totalUsage aggregated energy usage to attach to the response
	 * @return an {@link InsightDto} with the model output and the total usage
	 * @throws DependencyUnavailableException if the chat model cannot be reached
	 */
	private InsightDto buildInsight(final Long userId, final String prompt, final double totalUsage) {
		final ChatResponse response = callChatModel(userId, prompt);

		return InsightDto.builder()
				.userId(userId)
				.tips(response.getResult().getOutput().getText())
				.energyUsage(totalUsage)
				.build();
	}

	/**
	 * Calls the Ollama chat model with the given prompt, translating HTTP
	 * failures into a {@link DependencyUnavailableException}.
	 *
	 * @param userId identifier of the user, only used for logging
	 * @param prompt prompt to send to the model
	 * @return the response produced by the model
	 * @throws DependencyUnavailableException if the chat model is not reachable
	 */
	private ChatResponse callChatModel(final Long userId, final String prompt) {
		try {
			return ollamaChatModel.call(Prompt.builder().content(prompt).build());
		} catch (RestClientException e) {
			log.error("Chat model (Ollama) is unavailable for userId {}: {}", userId, e.getMessage());
			throw new DependencyUnavailableException(OLLAMA, e.getMessage(), e);
		}
	}

	/**
	 * Builds the prompt used to request personalized energy-saving tips.
	 *
	 * @param totalUsage total energy consumed over the last three days
	 * @return the prompt sent to the chat model
	 */
	private String getSavingPrompt(final double totalUsage) {
		return new StringBuilder()
				.append("This is my total consumption over the past 3 days.")
				.append("How can I reduce my energy consumption? How does it compare to average households?")
				.append("Total energu used: \n")
				.append(totalUsage)
				.toString();
	}

	/**
	 * Builds the prompt used to request an overview of the user's energy usage.
	 *
	 * @param usageData usage data retrieved from the Usage Service
	 * @return the prompt sent to the chat model
	 */
	private String getOverviewPrompt(final UsageDto usageData) {
		return new StringBuilder()
				.append("Analyse the following energy usage data and provide a ")
				.append("concise overview with actionable insights.")
				.append("This data is the aggregate data for the past 3 days.")
				.append("Usage Data: \n")
				.append(usageData.devices())
				.toString();
	}
}