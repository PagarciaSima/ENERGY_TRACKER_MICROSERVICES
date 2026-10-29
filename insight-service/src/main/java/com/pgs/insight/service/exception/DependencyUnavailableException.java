package com.pgs.insight.service.exception;

/**
 * Exception thrown when one of the downstream dependencies of this service
 * cannot be reached.
 * <p>
 * The service depends on two external collaborators: the <b>Usage Service</b>
 * (the aggregated consumption data) and the <b>Ollama chat model</b> (the
 * reasoning). When either of them is down the failure is not a bug of this
 * service, so it is reported as a {@code 503 Service Unavailable} instead of a
 * generic {@code 500 Internal Server Error}.
 */
public class DependencyUnavailableException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/** Name of the dependency that could not be reached, e.g. {@code "Ollama"}. */
	private final String dependency;

	/**
	 * Creates an exception for a dependency that is not available.
	 *
	 * @param dependency the dependency that is unavailable (for example
	 *                   {@code "Usage Service"} or {@code "Ollama"})
	 * @param detail     a human-readable detail of the failure, usually the
	 *                   message of the underlying HTTP exception
	 */
	public DependencyUnavailableException(String dependency, String detail) {
		super(dependency + " is unavailable: " + detail);
		this.dependency = dependency;
	}

	/**
	 * Creates an exception for a dependency that is not available, keeping the
	 * original failure as the cause.
	 *
	 * @param dependency the dependency that is unavailable (for example
	 *                   {@code "Usage Service"} or {@code "Ollama"})
	 * @param detail     a human-readable detail of the failure, usually the
	 *                   message of the underlying HTTP exception
	 * @param cause      the original exception raised by the HTTP client
	 */
	public DependencyUnavailableException(String dependency, String detail, Throwable cause) {
		super(dependency + " is unavailable: " + detail, cause);
		this.dependency = dependency;
	}

	/**
	 * Returns the name of the dependency that could not be reached.
	 *
	 * @return the dependency name, e.g. {@code "Ollama"}
	 */
	public String getDependency() {
		return dependency;
	}
}
