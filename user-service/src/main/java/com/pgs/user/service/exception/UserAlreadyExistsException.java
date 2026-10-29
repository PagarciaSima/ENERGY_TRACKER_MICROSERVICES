package com.pgs.user.service.exception;

/**
 * Exception thrown when trying to create a user whose email is already taken.
 */
public class UserAlreadyExistsException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates an exception for a duplicated user email.
	 *
	 * @param email the email that is already registered
	 */
	public UserAlreadyExistsException(String email) {
        super("User already exists with email: " + email);
    }
}
