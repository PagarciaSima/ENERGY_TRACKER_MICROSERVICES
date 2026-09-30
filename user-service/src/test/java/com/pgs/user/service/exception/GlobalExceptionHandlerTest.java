package com.pgs.user.service.exception;

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
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pgs.user.service.dto.ErrorResponse;

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
     * exception handlers through the full MVC stack.
     */
    @RestController
    static class TriggerController {

        @GetMapping("/trigger/not-found")
        public void notFound() {
            throw new UserNotFoundException(99L);
        }

        @GetMapping("/trigger/conflict")
        public void conflict() {
            throw new UserAlreadyExistsException("ana.garcia@example.com");
        }

        @GetMapping("/trigger/bad-request")
        public void badRequest() {
            throw validationException();
        }

        @GetMapping("/trigger/internal-error")
        public void internalError() {
            throw new IllegalStateException("boom");
        }

        /**
         * Builds a {@link MethodArgumentNotValidException} carrying a binding
         * result with a single {@code email} field error, so the validation
         * handler produces the expected message.
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
                    "trigger", "email", "", false, null, null, "must not be blank"));
            Message<String> message = MessageBuilder.withPayload("").build();
            return new MethodArgumentNotValidException(message, methodParameter, bindingResult);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  UserNotFoundException  -> 404 Not Found                            */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("UserNotFoundException")
    class UserNotFound {

        @Test
        void mapsToNotFoundWithErrorBody() throws Exception {
            mockMvc.perform(get("/trigger/not-found"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message").value("User not found with id: 99"));
        }
    }

    /* ------------------------------------------------------------------ */
    /*  UserAlreadyExistsException -> 409 Conflict                         */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("UserAlreadyExistsException")
    class UserAlreadyExists {

        @Test
        void mapsToConflictWithErrorBody() throws Exception {
            mockMvc.perform(get("/trigger/conflict"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.error").value("Conflict"))
                    .andExpect(jsonPath("$.message").value(
                            "User already exists with email: ana.garcia@example.com"));
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
                    .andExpect(jsonPath("$.message").value("email: must not be blank"));
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