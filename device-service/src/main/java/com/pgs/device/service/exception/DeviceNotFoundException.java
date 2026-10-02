package com.pgs.device.service.exception;

/**
 * Exception thrown when a requested device cannot be found.
 */
public class DeviceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception for a device that could not be found.
     *
     * @param id the ID of the device that was not found
     */
    public DeviceNotFoundException(Long id) {
        super("Device not found with id: " + id);
    }
}
