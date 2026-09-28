package com.pgs.user.service.controller;

import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pgs.user.service.dto.ErrorResponse;
import com.pgs.user.service.dto.PageResponse;
import com.pgs.user.service.dto.UserDto;
import com.pgs.user.service.dto.UserFilterDto;
import com.pgs.user.service.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for managing users.
 */
@Tag(name = "User", description = "Endpoints to manage the users of the Home Energy Tracker platform")
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	/*
	 * ============================================================ CREATE
	 * ============================================================
	 */

	@Operation(summary = "Create a user", description = "Creates a new user and returns the stored record including the auto-generated id.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "User created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class))),
			@ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(name = "badRequest", value = "{\"status\": 400, \"error\": \"Bad Request\", \"message\": \"email: must not be blank\"}"))),
			@ApiResponse(responseCode = "409", description = "User already exists", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(name = "conflict", value = "{\"status\": 409, \"error\": \"Conflict\", \"message\": \"User already exists with email: ana.garcia@example.com\"}"))),
			@ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))) })
	@PostMapping
	public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserDto userDto) {
		UserDto created = userService.createUser(userDto);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	/*
	 * ============================================================ READ - list all
	 * ============================================================
	 */

	@Operation(summary = "List all users", description = "Returns every stored user without pagination.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "List of users", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = UserDto.class)), examples = @ExampleObject(name = "usersList", value = "[{\"id\": 1, \"name\": \"Ana\", \"surname\": \"García\", \"email\": \"ana.garcia@example.com\", \"address\": \"Calle Mayor 5, Madrid\", \"alerting\": true, \"energyAlertingThreshold\": 3200.5}]"))),
			@ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))) })
	@GetMapping
	public ResponseEntity<List<UserDto>> getAllUsers() {
		return ResponseEntity.ok(userService.getAllUsers());
	}

	/*
	 * ============================================================ READ - paginated
	 * ============================================================
	 */

	@Operation(summary = "List users (paginated)", description = "Returns a page of users using the PageResponse wrapper.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "A page of users", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PageResponse.class), examples = @ExampleObject(name = "usersPage", value = "{\"content\": [{\"id\": 1, \"name\": \"Ana\", \"surname\": \"García\", \"email\": \"ana.garcia@example.com\", \"address\": \"Calle Mayor 5, Madrid\", \"alerting\": true, \"energyAlertingThreshold\": 3200.5}], \"page\": 0, \"size\": 10, \"totalElements\": 1, \"totalPages\": 1, \"last\": true, \"first\": true, \"empty\": false}"))),
			@ApiResponse(responseCode = "400", description = "Invalid pagination parameters", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))) })
	@GetMapping("/page")
	public ResponseEntity<PageResponse<UserDto>> getUsersPaginated(
			@Parameter(description = "Page index (zero-based)", example = "0") @RequestParam(defaultValue = "0") int page,
			@Parameter(description = "Page size", example = "10") @RequestParam(defaultValue = "10") int size) {
		return ResponseEntity.ok(userService.getUsersPaginated(page, size));
	}

	/*
	 * ============================================================ READ - search
	 * ============================================================
	 */

	@Operation(summary = "Search users by filters", description = "Searches users applying the optional filters and returns a paginated result. "
			+ "Optional filter query params: name, surname, email, address (fragments), "
			+ "alerting (boolean), minEnergyAlertingThreshold (number).")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "A page of matching users", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PageResponse.class), examples = @ExampleObject(name = "usersSearch", value = "{\"content\": [{\"id\": 1, \"name\": \"Ana\", \"surname\": \"García\", \"email\": \"ana.garcia@example.com\", \"address\": \"Calle Mayor 5, Madrid\", \"alerting\": true, \"energyAlertingThreshold\": 3200.5}], \"page\": 0, \"size\": 10, \"totalElements\": 1, \"totalPages\": 1, \"last\": true, \"first\": true, \"empty\": false}"))),
			@ApiResponse(responseCode = "400", description = "Invalid filter or pagination parameters", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))) })
	@GetMapping("/search")
	public ResponseEntity<PageResponse<UserDto>> searchUsers(@ParameterObject @ModelAttribute UserFilterDto filter,
			@Parameter(description = "Page index (zero-based)", example = "0") @RequestParam(defaultValue = "0") int page,
			@Parameter(description = "Page size", example = "10") @RequestParam(defaultValue = "10") int size) {
		return ResponseEntity.ok(userService.searchUsers(filter, page, size));
	}

	/*
	 * ============================================================ READ - by id
	 * ============================================================
	 */

	@Operation(summary = "Get a user by id", description = "Returns the user with the given id, or a 404 error body when it does not exist.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "The requested user", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class))),
			@ApiResponse(responseCode = "404", description = "User not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(name = "notFound", value = "{\"status\": 404, \"error\": \"Not Found\", \"message\": \"User not found with id: 99\"}"))),
			@ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))) })
	@GetMapping("/{id}")
	public ResponseEntity<UserDto> getUserById(
			@Parameter(description = "User id", example = "1") @PathVariable Long id) {
		return ResponseEntity.ok(userService.getUserById(id));
	}

	/*
	 * ============================================================ UPDATE
	 * ============================================================
	 */

	@Operation(summary = "Update a user", description = "Replaces the data of the user with the given id and returns the updated record.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "User updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class))),
			@ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "404", description = "User not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(name = "notFound", value = "{\"status\": 404, \"error\": \"Not Found\", \"message\": \"User not found with id: 99\"}"))),
			@ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))) })
	@PutMapping("/{id}")
	public ResponseEntity<UserDto> updateUser(@Parameter(description = "User id", example = "1") @PathVariable Long id,
			@Valid @RequestBody UserDto userDto) {
		return ResponseEntity.ok(userService.updateUser(id, userDto));
	}

	/*
	 * ============================================================ DELETE
	 * ============================================================
	 */

	@Operation(summary = "Delete a user", description = "Deletes the user with the given id. Returns 204 on success, "
			+ "or a 404 error body when the user does not exist.")
	@ApiResponses({ @ApiResponse(responseCode = "204", description = "User deleted, no content"),
			@ApiResponse(responseCode = "404", description = "User not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(name = "notFound", value = "{\"status\": 404, \"error\": \"Not Found\", \"message\": \"User not found with id: 99\"}"))),
			@ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))) })
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteUser(@Parameter(description = "User id", example = "1") @PathVariable Long id) {
		userService.deleteUser(id);
		return ResponseEntity.noContent().build();
	}
}