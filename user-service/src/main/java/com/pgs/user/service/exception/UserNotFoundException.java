package com.pgs.user.service.exception;

public class UserNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public UserNotFoundException(Long id) {
        super("User not found with id: " + id);
    }
}