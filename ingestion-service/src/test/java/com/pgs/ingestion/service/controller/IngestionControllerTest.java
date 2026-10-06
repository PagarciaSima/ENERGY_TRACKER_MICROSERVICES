package com.pgs.ingestion.service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.pgs.ingestion.service.dto.EnergyUsageDto;
import com.pgs.ingestion.service.service.IngestionService;

/**
 * Web-layer slice tests for {@link IngestionController} using {@link WebMvcTest}.
 * <p>
 * The {@link IngestionService} is mocked so no Kafka broker is required. The
 * {@code GlobalExceptionHandler} advice is picked up from the application
 * context, so {@code @Valid} rejection responses (400) are asserted against the
 * real {@code ErrorResponse} body.
 */
@WebMvcTest(IngestionController.class)
class IngestionControllerTest {

	private static final String BASE_URL = "/api/v1/ingestion";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private IngestionService ingestionService;

	/* ------------------------------------------------------------------ */
	/*  Request body helpers                                               */
	/* ------------------------------------------------------------------ */

	private static String validJsonBody() {
		return """
				{
				  "deviceId": 42,
				  "energyConsumed": 3.5,
				  "timestamp": "2026-10-04T15:30:45Z"
				}
				""";
	}

	/* ------------------------------------------------------------------ */
	/*  POST /api/v1/ingestion (happy path)                                 */
	/* ------------------------------------------------------------------ */

	@Nested
	@DisplayName("ingestData happy path")
	class IngestHappyPath {

		@Test
		void forValidReading_returns201AndDelegatesToService() throws Exception {
			mockMvc.perform(post(BASE_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content(validJsonBody()))
					.andExpect(status().isCreated());

			verify(ingestionService).ingestEnergyUsage(any(EnergyUsageDto.class));
		}
	}

	/* ------------------------------------------------------------------ */
	/*  POST /api/v1/ingestion (validation failures -> 400)                */
	/* ------------------------------------------------------------------ */

	@Nested
	@DisplayName("ingestData validation")
	class IngestValidation {

		@Test
		void whenDeviceIdMissing_returnsBadRequest() throws Exception {
			mockMvc.perform(post(BASE_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									  "energyConsumed": 3.5,
									  "timestamp": "2026-10-04T15:30:45Z"
									}
									"""))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.status").value(400))
					.andExpect(jsonPath("$.error").value("Bad Request"))
					.andExpect(jsonPath("$.message").value("deviceId: deviceId must not be null"));
		}

		@Test
		void whenTimestampMissing_returnsBadRequest() throws Exception {
			mockMvc.perform(post(BASE_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									  "deviceId": 42,
									  "energyConsumed": 3.5
									}
									"""))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.status").value(400))
					.andExpect(jsonPath("$.message").value("timestamp: timestamp must not be null"));
		}

		@Test
		void whenEnergyConsumedNegative_returnsBadRequest() throws Exception {
			mockMvc.perform(post(BASE_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									  "deviceId": 42,
									  "energyConsumed": -1.0,
									  "timestamp": "2026-10-04T15:30:45Z"
									}
									"""))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.status").value(400))
					.andExpect(jsonPath("$.message")
							.value("energyConsumed: energyConsumed must be zero or positive"));
		}

		@Test
		void whenTimestampInFuture_returnsBadRequest() throws Exception {
			mockMvc.perform(post(BASE_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									  "deviceId": 42,
									  "energyConsumed": 3.5,
									  "timestamp": "2999-01-01T00:00:00Z"
									}
									"""))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.status").value(400))
					.andExpect(jsonPath("$.message").value("timestamp: timestamp must not be in the future"));
		}

		@Test
		void whenMalformedJson_returnsBadRequest() throws Exception {
			mockMvc.perform(post(BASE_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content("{ not valid json"))
					.andExpect(status().isBadRequest());
		}
	}
}