package com.pgs.insight.service.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.pgs.insight.service.dto.UsageDto;

/**
 * HTTP client used to retrieve the aggregated energy usage of a user from the
 * Usage Service.
 * <p>
 * This component acts as an anti-corruption layer between the current service
 * and the {@code usage-service}, hiding the details of the HTTP communication
 * (URL building, serialization, status handling) from the business logic.
 * <p>
 * It is backed by a {@link RestClient} bean (see
 * {@link com.pgs.insight.service.config.RestClientConfig}) whose base URL comes
 * from the {@code usage.service.url} property, which allows different values
 * per environment (local, Docker, production).
 */
@Component
public class UsageClient {

	private final RestClient restClient;

	/**
	 * Creates a new client targeting the given Usage Service base URL.
	 *
	 * @param restClient the {@code RestClient} pre-configured with the Usage
	 *                   Service base URL ({@code usage.service.url}), for
	 *                   example {@code http://localhost:8083/api/v1/usage} or
	 *                   {@code http://usage-service:8083/api/v1/usage}
	 */
	public UsageClient(@Qualifier("usageRestClient") RestClient restClient) {
		this.restClient = restClient;
	}

	/**
	 * Retrieves the aggregated energy usage of a user over the last given number
	 * of days.
	 * <p>
	 * Performs a {@code GET} request to {@code {baseUrl}/{userId}?days={days}} and
	 * deserializes the JSON response into a {@link UsageDto}.
	 *
	 * @param userId identifier of the user whose usage is to be retrieved
	 * @param days   number of days to aggregate
	 * @return the usage data returned by the Usage Service, or {@code null} if
	 *         the response body is empty
	 * @throws org.springframework.web.client.RestClientException if the HTTP
	 *         request fails (for example, connection error or 5xx response)
	 * @throws org.springframework.web.client.HttpClientErrorException if the
	 *         Usage Service returns a 4xx response (for example, 404 Not Found)
	 */
	public UsageDto getXDaysUsageForUser(Long userId, int days) {
		return restClient.get()
				.uri("/{userId}?days={days}", userId, days)
				.retrieve()
				.body(UsageDto.class);
	}
}
