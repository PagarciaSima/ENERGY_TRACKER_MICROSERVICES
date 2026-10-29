package com.pgs.user.service.exception;

/**
 * Exception thrown when a requested user cannot be found.
 */
public class UserNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates an exception for a user that could not be found.
	 *
	 * @param id the ID of the user that was not found
	 */
	public UserNotFoundException(Long id) {
        super("User not found with id: " + id);
    }
}