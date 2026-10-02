package com.pgs.device.service.controller;

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

import com.pgs.device.service.dto.DeviceDto;
import com.pgs.device.service.dto.DeviceFilterDto;
import com.pgs.device.service.dto.ErrorResponse;
import com.pgs.device.service.dto.PageResponse;
import com.pgs.device.service.service.DeviceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for managing devices of the Home Energy Tracker platform.
 * <p>
 * All endpoints are exposed under {@code /api/v1/device}. Devices belong to a
 * user (referenced by {@code userId}) and have a name, a {@link com.pgs.device.service.model.DeviceType type}
 * and a location.
 */
@Tag(name = "Device", description = "Endpoints to manage the devices of the Home Energy Tracker platform")
@RestController
@RequestMapping("/api/v1/device")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    /* ------------------------------------------------------------------ */
    /*  POST /api/v1/device                                                */
    /* ------------------------------------------------------------------ */

    /**
     * Creates a new device.
     *
     * @param input the device data to create
     * @return the created device with HTTP 201
     */
    @Operation(summary = "Create a device",
            description = "Creates a new device and returns the stored record including the auto-generated id.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Device created successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = DeviceDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "badRequest",
                                    value = "{\"status\":400,\"error\":\"Bad Request\",\"message\":\"name: must not be blank\"}"))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<DeviceDto> createDevice(@Valid @RequestBody DeviceDto input) {
        DeviceDto created = deviceService.createDevice(input);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /* ------------------------------------------------------------------ */
    /*  GET /api/v1/device                                                 */
    /* ------------------------------------------------------------------ */

    /**
     * Lists all devices, paginated.
     *
     * @param page the page index (zero-based)
     * @param size the page size
     * @return a page of devices with HTTP 200
     */
    @Operation(summary = "List all devices (paginated)",
            description = "Returns a page of every stored device using the PageResponse wrapper.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A page of devices",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @GetMapping
    public ResponseEntity<PageResponse<DeviceDto>> getAllDevices(
            @Parameter(description = "Page index (zero-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(deviceService.getAllDevices(page, size));
    }

    /* ------------------------------------------------------------------ */
    /*  GET /api/v1/device/search                                          */
    /* ------------------------------------------------------------------ */

    /**
     * Searches devices by optional filters, paginated.
     *
     * @param filter the filter criteria
     * @param page   the page index (zero-based)
     * @param size   the page size
     * @return a page of matching devices with HTTP 200
     */
    @Operation(summary = "Search devices by filters",
            description = "Searches devices applying the optional filters and returns a paginated result. "
                    + "Optional filter query params: name, type, location, userId.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A page of matching devices",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid filter or pagination parameters"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @GetMapping("/search")
    public ResponseEntity<PageResponse<DeviceDto>> searchDevices(
            @Parameter(hidden = true) @ModelAttribute DeviceFilterDto filter,
            @Parameter(description = "Page index (zero-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(deviceService.searchDevices(filter, page, size));
    }

    /* ------------------------------------------------------------------ */
    /*  GET /api/v1/device/{id}                                            */
    /* ------------------------------------------------------------------ */

    /**
     * Retrieves a device by ID, using cache when available.
     *
     * @param id the device ID
     * @return the device with HTTP 200, or HTTP 404 if not found
     */
    @Operation(summary = "Get a device by id",
            description = "Returns the device with the given id, or a 404 error body when it does not exist.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The requested device",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = DeviceDto.class))),
            @ApiResponse(responseCode = "404", description = "Device not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "notFound",
                                    value = "{\"status\":404,\"error\":\"Not Found\",\"message\":\"Device not found with id: 99\"}"))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @GetMapping("/{id}")
    public ResponseEntity<DeviceDto> getDeviceById(
            @Parameter(description = "Device id", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(deviceService.getDeviceById(id));
    }

    /* ------------------------------------------------------------------ */
    /*  PUT /api/v1/device/{id}                                            */
    /* ------------------------------------------------------------------ */

    /**
     * Updates an existing device.
     *
     * @param id    the device ID
     * @param input the updated device data
     * @return the updated device with HTTP 200, or HTTP 404 if not found
     */
    @Operation(summary = "Update a device",
            description = "Replaces the data of the device with the given id and returns the updated record.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Device updated successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = DeviceDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "404", description = "Device not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @PutMapping("/{id}")
    public ResponseEntity<DeviceDto> updateDevice(
            @Parameter(description = "Device id", example = "1") @PathVariable Long id,
            @Valid @RequestBody DeviceDto input) {
        return ResponseEntity.ok(deviceService.updateDevice(id, input));
    }

    /* ------------------------------------------------------------------ */
    /*  DELETE /api/v1/device/{id}                                         */
    /* ------------------------------------------------------------------ */

    /**
     * Deletes a device by ID.
     *
     * @param id the device ID
     * @return HTTP 204 if deleted, or HTTP 404 if not found
     */
    @Operation(summary = "Delete a device",
            description = "Deletes the device with the given id. Returns 204 on success.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Device deleted, no content"),
            @ApiResponse(responseCode = "404", description = "Device not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(
            @Parameter(description = "Device id", example = "1") @PathVariable Long id) {
        deviceService.deleteDevice(id);
        return ResponseEntity.noContent().build();
    }

    /* ------------------------------------------------------------------ */
    /*  GET /api/v1/device/user/{userId}                                   */
    /* ------------------------------------------------------------------ */

    /**
     * Lists all devices belonging to a user, paginated.
     *
     * @param userId the user ID
     * @param page   the page index (zero-based)
     * @param size   the page size
     * @return a page of devices belonging to the user with HTTP 200
     */
    @Operation(summary = "List devices by user id",
            description = "Returns a paginated list of the devices owned by the given user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A page of devices for the user",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<PageResponse<DeviceDto>> getDevicesByUserId(
            @Parameter(description = "User id", example = "1") @PathVariable Long userId,
            @Parameter(description = "Page index (zero-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(deviceService.getDevicesByUserId(userId, page, size));
    }
}