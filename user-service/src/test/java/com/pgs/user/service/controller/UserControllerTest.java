package com.pgs.user.service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.pgs.user.service.dto.PageResponse;
import com.pgs.user.service.dto.UserDto;
import com.pgs.user.service.dto.UserFilterDto;
import com.pgs.user.service.exception.UserAlreadyExistsException;
import com.pgs.user.service.exception.UserNotFoundException;
import com.pgs.user.service.service.UserService;

/**
 * Web-layer slice tests for {@link UserController} using {@link WebMvcTest}.
 * <p>
 * The {@link UserService} is mocked so no database or Redis is required. The
 * {@code GlobalExceptionHandler} advice is picked up from the application
 * context, so error responses (404 / 409) are asserted against the real
 * {@code ErrorResponse} body.
 */
@WebMvcTest(UserController.class)
class UserControllerTest {

    private static final String NAME = "Ana";
    private static final String SURNAME = "García";
    private static final String EMAIL = "ana.garcia@example.com";
    private static final String ADDRESS = "Calle Mayor 5, Madrid";
    private static final boolean ALERTING = true;
    private static final double THRESHOLD = 3200.5;

    private static final String BASE_URL = "/api/v1/user";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    /* ------------------------------------------------------------------ */
    /*  Test data helpers                                                  */
    /* ------------------------------------------------------------------ */

    private static UserDto existingUserDto(Long id) {
        return UserDto.builder()
                .id(id)
                .name(NAME).surname(SURNAME).email(EMAIL).address(ADDRESS)
                .alerting(ALERTING).energyAlertingThreshold(THRESHOLD)
                .build();
    }

    private static PageResponse<UserDto> singlePage() {
        Page<UserDto> page = new PageImpl<>(
                List.of(existingUserDto(1L)), PageRequest.of(0, 10), 1L);
        return PageResponse.of(page);
    }

    private static String userJsonBody() {
        return """
                {
                  "name": "Ana",
                  "surname": "García",
                  "email": "ana.garcia@example.com",
                  "address": "Calle Mayor 5, Madrid",
                  "alerting": true,
                  "energyAlertingThreshold": 3200.5
                }
                """;
    }

    /* ------------------------------------------------------------------ */
    /*  POST /api/v1/user                                                  */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("createUser")
    class CreateUser {

        @Test
        void whenValidBody_returnsCreatedWithBody() throws Exception {
            when(userService.createUser(any(UserDto.class)))
                    .thenReturn(existingUserDto(1L));

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(userJsonBody()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value(NAME))
                    .andExpect(jsonPath("$.email").value(EMAIL));
        }

        @Test
        void whenEmailAlreadyExists_returnsConflict() throws Exception {
            when(userService.createUser(any(UserDto.class)))
                    .thenThrow(new UserAlreadyExistsException(EMAIL));

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(userJsonBody()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.error").value("Conflict"))
                    .andExpect(jsonPath("$.message").value(
                            "User already exists with email: " + EMAIL));
        }
    }

    /* ------------------------------------------------------------------ */
    /*  GET /api/v1/user                                                   */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("getAllUsers")
    class GetAllUsers {

        @Test
        void returnsAllUsers() throws Exception {
            when(userService.getAllUsers()).thenReturn(List.of(existingUserDto(1L)));

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].name").value(NAME));
        }
    }

    /* ------------------------------------------------------------------ */
    /*  GET /api/v1/user/page                                              */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("getUsersPaginated")
    class GetUsersPaginated {

        @Test
        void returnsPaginatedPage() throws Exception {
            when(userService.getUsersPaginated(0, 10)).thenReturn(singlePage());

            mockMvc.perform(get(BASE_URL + "/page")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(10))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.first").value(true));
        }
    }

    /* ------------------------------------------------------------------ */
    /*  GET /api/v1/user/search                                            */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("searchUsers")
    class SearchUsers {

        @Test
        void returnsMatchingPage() throws Exception {
            // any(UserFilterDto.class) + anyInt() for both ints:
            // all arguments must be matchers (no mixing with literals).
            when(userService.searchUsers(any(UserFilterDto.class), anyInt(), anyInt()))
                    .thenReturn(singlePage());

            mockMvc.perform(get(BASE_URL + "/search")
                            .param("name", "An")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    /* ------------------------------------------------------------------ */
    /*  GET /api/v1/user/{id}                                              */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("getUserById")
    class GetUserById {

        @Test
        void whenExists_returnsUser() throws Exception {
            when(userService.getUserById(1L)).thenReturn(existingUserDto(1L));

            mockMvc.perform(get(BASE_URL + "/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.email").value(EMAIL));
        }

        @Test
        void whenMissing_returnsNotFound() throws Exception {
            when(userService.getUserById(99L))
                    .thenThrow(new UserNotFoundException(99L));

            mockMvc.perform(get(BASE_URL + "/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("User not found with id: 99"));
        }
    }

    /* ------------------------------------------------------------------ */
    /*  PUT /api/v1/user/{id}                                              */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("updateUser")
    class UpdateUser {

        @Test
        void whenExists_returnsUpdatedUser() throws Exception {
            when(userService.updateUser(any(Long.class), any(UserDto.class)))
                    .thenReturn(existingUserDto(1L));

            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(userJsonBody()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value(NAME));
        }
    }

    /* ------------------------------------------------------------------ */
    /*  PATCH /api/v1/user/{id}/alerting                                    */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("setAlertingEnabled")
    class SetAlertingEnabled {

        @Test
        void whenEnabled_changesAlertingFlagAndReturnsUser() throws Exception {
            when(userService.setAlertingEnabled(1L, false))
                    .thenReturn(existingUserDto(1L));

            mockMvc.perform(patch(BASE_URL + "/1/alerting")
                            .param("enabled", "false"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));

            verify(userService).setAlertingEnabled(1L, false);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  DELETE /api/v1/user/{id}                                           */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("deleteUser")
    class DeleteUser {

        @Test
        void whenExists_returnsNoContent() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/1"))
                    .andExpect(status().isNoContent());

            verify(userService).deleteUser(1L);
        }
    }
}