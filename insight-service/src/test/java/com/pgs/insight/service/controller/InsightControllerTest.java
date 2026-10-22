package com.pgs.insight.service.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

import com.pgs.insight.service.dto.InsightDto;
import com.pgs.insight.service.service.InsightService;

/**
 * Web-layer slice tests for {@link InsightController} using
 * {@link WebMvcTest}.
 *
 * <p>
 * The {@link InsightService} is mocked so neither the Usage Service nor an
 * Ollama instance is required. The {@code GlobalExceptionHandler} advice is
 * picked up from the application context, so the error contract documented in
 * the OpenAPI specification is asserted against the real {@code ErrorResponse}
 * body.
 */
@WebMvcTest(InsightController.class)
@DisplayName("InsightController")
class InsightControllerTest {

	private static final String BASE_URL = "/api/v1/insight";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private InsightService insightService;

	private static InsightDto insight() {
		return InsightDto.builder()
				.userId(42L)
				.tips("Consider shifting heavy loads to off-peak hours.")
				.energyUsage(12.75)
				.build();
	}

	/* ------------------------------------------------------------------ */
	/*  GET /api/v1/insight/saving-tips/{userId}                           */
	/* ------------------------------------------------------------------ */

	@Nested
	@DisplayName("GET /saving-tips/{userId}")
	class GetSavingTips {

		@Test
		@DisplayName("returns 200 with the insight body")
		void returns200WithInsightBody() throws Exception {
			when(insightService.getSavingsTips(42L)).thenReturn(insight());

			mockMvc.perform(get(BASE_URL + "/saving-tips/42"))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andExpect(jsonPath("$.userId").value(42))
					.andExpect(jsonPath("$.tips")
							.value("Consider shifting heavy loads to off-peak hours."))
					.andExpect(jsonPath("$.energyUsage").value(12.75));

			verify(insightService).getSavingsTips(42L);
		}

		@Test
		@DisplayName("returns 400 when the userId is not a number")
		void returns400WhenUserIdIsNotANumber() throws Exception {
			mockMvc.perform(get(BASE_URL + "/saving-tips/abc"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.status").value(400))
					.andExpect(jsonPath("$.error").value("Bad Request"))
					.andExpect(jsonPath("$.message").value("Parameter 'userId' must be of type 'Long'"));
		}

		@Test
		@DisplayName("returns 500 when the service fails")
		void returns500WhenServiceFails() throws Exception {
			when(insightService.getSavingsTips(anyLong()))
					.thenThrow(new IllegalStateException("boom"));

			mockMvc.perform(get(BASE_URL + "/saving-tips/42"))
					.andExpect(status().isInternalServerError())
					.andExpect(jsonPath("$.status").value(500))
					.andExpect(jsonPath("$.error").value("Internal Server Error"))
					.andExpect(jsonPath("$.message").value("Unexpected error"));
		}
	}

	/* ------------------------------------------------------------------ */
	/*  GET /api/v1/insight/overview/{userId}                              */
	/* ------------------------------------------------------------------ */

	@Nested
	@DisplayName("GET /overview/{userId}")
	class GetOverview {

		@Test
		@DisplayName("returns 200 with the insight body")
		void returns200WithInsightBody() throws Exception {
			when(insightService.getOverview(42L)).thenReturn(insight());

			mockMvc.perform(get(BASE_URL + "/overview/42"))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andExpect(jsonPath("$.userId").value(42))
					.andExpect(jsonPath("$.energyUsage").value(12.75));

			verify(insightService).getOverview(42L);
		}

		@Test
		@DisplayName("returns 400 when the userId is not a number")
		void returns400WhenUserIdIsNotANumber() throws Exception {
			mockMvc.perform(get(BASE_URL + "/overview/abc"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.status").value(400))
					.andExpect(jsonPath("$.error").value("Bad Request"))
					.andExpect(jsonPath("$.message").value("Parameter 'userId' must be of type 'Long'"));
		}

		@Test
		@DisplayName("returns 500 when the service fails")
		void returns500WhenServiceFails() throws Exception {
			when(insightService.getOverview(anyLong()))
					.thenThrow(new IllegalStateException("boom"));

			mockMvc.perform(get(BASE_URL + "/overview/42"))
					.andExpect(status().isInternalServerError())
					.andExpect(jsonPath("$.status").value(500))
					.andExpect(jsonPath("$.error").value("Internal Server Error"))
					.andExpect(jsonPath("$.message").value("Unexpected error"));
		}
	}
}
