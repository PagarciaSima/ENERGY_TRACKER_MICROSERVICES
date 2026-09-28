package com.pgs.user.service.controller;

import java.util.List;

import org.springframework.data.domain.Page;
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

import com.pgs.user.service.dto.UserDto;
import com.pgs.user.service.dto.UserFilterDto;
import com.pgs.user.service.service.UserService;

import lombok.RequiredArgsConstructor;

/**
 * REST controller for managing users.
 */
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Creates a new user.
     *
     * @param userDto the user data to create
     * @return the created user with HTTP 201
     */
    @PostMapping
    public ResponseEntity<UserDto> createUser(@RequestBody UserDto userDto) {
        UserDto created = userService.createUser(userDto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Retrieves all users.
     *
     * @return the list of users with HTTP 200
     */
    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Retrieves a paginated list of users.
     *
     * @param page the page index (zero-based)
     * @param size the page size
     * @return a page of users with HTTP 200
     */
    @GetMapping("/page")
    public ResponseEntity<Page<UserDto>> getUsersPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(userService.getUsersPaginated(page, size));
    }

    /**
     * Searches users by optional filters, paginated.
     *
     * @param filter the filter criteria
     * @param page   the page index (zero-based)
     * @param size   the page size
     * @return a page of matching users with HTTP 200
     */
    @GetMapping("/search")
    public ResponseEntity<Page<UserDto>> searchUsers(
            @ModelAttribute UserFilterDto filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(userService.searchUsers(filter, page, size));
    }

    /**
     * Retrieves a user by ID.
     *
     * @param id the user ID
     * @return the user with HTTP 200, or HTTP 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        UserDto userDto = userService.getUserById(id);
        if (userDto == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(userDto);
    }

    /**
     * Updates an existing user.
     *
     * @param id      the user ID
     * @param userDto the updated user data
     * @return HTTP 200 with a success message, or HTTP 404 if not found
     */
    @PutMapping("/{id}")
    public ResponseEntity<String> updateUser(@PathVariable Long id,
                                              @RequestBody UserDto userDto) {
        try {
            userService.updateUser(id, userDto);
            return ResponseEntity.ok("User updated successfully");
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Deletes a user by ID.
     *
     * @param id the user ID
     * @return HTTP 204 if deleted, or HTTP 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

}