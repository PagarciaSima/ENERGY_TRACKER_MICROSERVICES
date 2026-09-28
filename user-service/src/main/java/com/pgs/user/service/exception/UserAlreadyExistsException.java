package com.pgs.user.service.exception;

public class UserAlreadyExistsException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public UserAlreadyExistsException(String email) {
        super("User already exists with email: " + email);
    }
}
