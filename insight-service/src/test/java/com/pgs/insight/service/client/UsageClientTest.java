package com.pgs.insight.service.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import com.pgs.insight.service.dto.UsageDto;

/**
 * Unit tests for {@link UsageClient}. The HTTP layer is mocked with
 * {@link MockRestServiceServer}, so no real Usage Service is required.
 *
 * <p>
 * The tests verify URL building (the {@code days} query parameter), JSON
 * deserialization, the mapping of HTTP status codes to exceptions and the
 * empty-body handling (returns {@code null}).
 */
@DisplayName("UsageClient")
class UsageClientTest {

	private static final String BASE_URL = "http://usage-service:8083/api/v1/usage";

	private MockRestServiceServer server;
	private UsageClient usageClient;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		usageClient = new UsageClient(builder.baseUrl(BASE_URL).build());
	}

	/* ------------------------------------------------------------------ */
	/*  getXDaysUsageForUser (happy path)                                  */
	/* ------------------------------------------------------------------ */

	@Nested
	@DisplayName("getXDaysUsageForUser")
	class GetXDaysUsageForUser {

		@Test
		@DisplayName("returns the deserialized usage on 200")
		void returnsDeserializedUsageOn200() {
			String json = """
					{
					  "userId": 42,
					  "devices": [
					    {"id":1,"name":"Bulb","type":"LIGHT","location":"Salon","energyConsumed":3.5},
					    {"id":2,"name":"Thermostat","type":"CLIMATE","location":"Cocina","energyConsumed":1.25}
					  ]
					}
					""";
			server.expect(requestTo(BASE_URL + "/42?days=3"))
					.andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

			UsageDto usage = usageClient.getXDaysUsageForUser(42L, 3);

			assertThat(usage).isNotNull();
			assertThat(usage.userId()).isEqualTo(42L);
			assertThat(usage.devices()).hasSize(2);
			assertThat(usage.devices().get(0).id()).isEqualTo(1L);
			assertThat(usage.devices().get(0).name()).isEqualTo("Bulb");
			assertThat(usage.devices().get(0).energyConsumed()).isEqualTo(3.5);
			assertThat(usage.devices().get(1).id()).isEqualTo(2L);
			assertThat(usage.devices().get(1).energyConsumed()).isEqualTo(1.25);
			server.verify();
		}

		@Test
		@DisplayName("performs a GET request to {baseUrl}/{userId}?days={days}")
		void performsGetRequestToExpectedUrl() {
			server.expect(requestTo(BASE_URL + "/7?days=30"))
					.andExpect(method(HttpMethod.GET))
					.andRespond(withSuccess("{\"userId\":7,\"devices\":[]}", MediaType.APPLICATION_JSON));

			usageClient.getXDaysUsageForUser(7L, 30);

			server.verify();
		}

		@Test
		@DisplayName("returns null when the response body is empty")
		void returnsNullWhenBodyEmpty() {
			server.expect(requestTo(BASE_URL + "/42?days=3"))
					.andRespond(withSuccess("", MediaType.APPLICATION_JSON));

			assertThat(usageClient.getXDaysUsageForUser(42L, 3)).isNull();
			server.verify();
		}

		@Test
		@DisplayName("throws HttpClientErrorException.NotFound on 404")
		void throwsNotFoundOn404() {
			server.expect(requestTo(BASE_URL + "/999?days=3"))
					.andRespond(withStatus(HttpStatus.NOT_FOUND));

			assertThatThrownBy(() -> usageClient.getXDaysUsageForUser(999L, 3))
					.isInstanceOf(HttpClientErrorException.NotFound.class);
			server.verify();
		}

		@Test
		@DisplayName("throws HttpServerErrorException on 500")
		void throwsServerErrorOn500() {
			server.expect(requestTo(BASE_URL + "/1?days=3"))
					.andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

			assertThatThrownBy(() -> usageClient.getXDaysUsageForUser(1L, 3))
					.isInstanceOf(HttpServerErrorException.class);
			server.verify();
		}
	}
}
