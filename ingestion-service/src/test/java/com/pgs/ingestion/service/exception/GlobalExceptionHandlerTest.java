package com.pgs.ingestion.service.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pgs.ingestion.service.dto.ErrorResponse;

/**
 * Slice tests for {@link GlobalExceptionHandler} exercised implicitly through
 * {@link WebMvcTest}. Each endpoint of the {@link TriggerController} throws one
 * of the exceptions handled by the {@code @RestControllerAdvice}, and the tests
 * assert the {@link ErrorResponse} JSON body produced by the exception handler.
 */
@WebMvcTest(GlobalExceptionHandlerTest.TriggerController.class)
class GlobalExceptionHandlerTest {

	@Autowired
	private MockMvc mockMvc;

	/**
	 * Minimal controller used only to trigger the {@code GlobalExceptionHandler}
	 * handlers through the full MVC stack.
	 */
	@RestController
	static class TriggerController {

		@GetMapping("/trigger/bad-request")
		public void badRequest() throws MethodArgumentNotValidException {
			throw validationException();
		}

		@GetMapping("/trigger/illegal-argument")
		public void illegalArgument() {
			throw new IllegalArgumentException("deviceId must be positive");
		}

		@GetMapping("/trigger/internal-error")
		public void internalError() {
			throw new IllegalStateException("boom");
		}

		/**
		 * Builds a {@link MethodArgumentNotValidException} (the exact type produced
		 * by {@code @Valid} on MVC request bodies) carrying a binding result with a
		 * single {@code deviceId} field error, so the validation handler produces
		 * the expected message.
		 */
		private static MethodArgumentNotValidException validationException() {
			Method method;
			try {
				method = TriggerController.class.getDeclaredMethod("badRequest");
			} catch (NoSuchMethodException e) {
				throw new IllegalStateException(e);
			}
			MethodParameter methodParameter = new MethodParameter(method, -1);
			BeanPropertyBindingResult bindingResult =
					new BeanPropertyBindingResult(TriggerController.class, "trigger");
			bindingResult.addError(new FieldError(
					"trigger", "deviceId", "", false, null, null, "deviceId must not be null"));
			return new MethodArgumentNotValidException(methodParameter, bindingResult);
		}
	}

	/* ------------------------------------------------------------------ */
	/*  MethodArgumentNotValidException -> 400 Bad Request                 */
	/* ------------------------------------------------------------------ */

	@Nested
	@DisplayName("MethodArgumentNotValidException")
	class Validation {

		@Test
		void mapsToBadRequestJoiningFieldErrors() throws Exception {
			mockMvc.perform(get("/trigger/bad-request"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.status").value(400))
					.andExpect(jsonPath("$.error").value("Bad Request"))
					.andExpect(jsonPath("$.message").value("deviceId: deviceId must not be null"));
		}
	}

	/* ------------------------------------------------------------------ */
	/*  IllegalArgumentException -> 400 Bad Request                        */
	/* ------------------------------------------------------------------ */

	@Nested
	@DisplayName("IllegalArgumentException")
	class IllegalArgument {

		@Test
		void mapsToBadRequestWithMessage() throws Exception {
			mockMvc.perform(get("/trigger/illegal-argument"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.status").value(400))
					.andExpect(jsonPath("$.error").value("Bad Request"))
					.andExpect(jsonPath("$.message").value("deviceId must be positive"));
		}
	}

	/* ------------------------------------------------------------------ */
	/*  Generic Exception -> 500 Internal Server Error                     */
	/* ------------------------------------------------------------------ */

	@Nested
	@DisplayName("generic Exception")
	class Generic {

		@Test
		void mapsToInternalServerErrorWithGenericMessage() throws Exception {
			mockMvc.perform(get("/trigger/internal-error"))
					.andExpect(status().isInternalServerError())
					.andExpect(jsonPath("$.status").value(500))
					.andExpect(jsonPath("$.error").value("Internal Server Error"))
					.andExpect(jsonPath("$.message").value("Unexpected error"));
		}
	}
}