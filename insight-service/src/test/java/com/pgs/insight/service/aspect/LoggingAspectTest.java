package com.pgs.insight.service.aspect;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link LoggingAspect}.
 *
 * <p>
 * The {@link JoinPoint} is mocked so the advice methods can be exercised in
 * isolation, without requiring a Spring context or an AOP proxy.
 */
@DisplayName("LoggingAspect")
class LoggingAspectTest {

	private JoinPoint joinPoint;
	private LoggingAspect aspect;

	@BeforeEach
	void setUp() {
		joinPoint = mock(JoinPoint.class);
		Signature signature = mock(Signature.class);
		when(signature.getName()).thenReturn("getSavingsTips");
		when(joinPoint.getSignature()).thenReturn(signature);
		when(joinPoint.getArgs()).thenReturn(new Object[] { 42L });
		aspect = new LoggingAspect();
	}

	@Nested
	@DisplayName("logBefore")
	class LogBefore {

		@Test
		@DisplayName("completes without throwing for a normal join point")
		void completesWithoutThrowing() {
			assertThatCode(() -> aspect.logBefore(joinPoint)).doesNotThrowAnyException();
		}
	}

	@Nested
	@DisplayName("logAfterReturning")
	class LogAfterReturning {

		@Test
		@DisplayName("completes without throwing for a normal join point")
		void completesWithoutThrowing() {
			assertThatCode(() -> aspect.logAfterReturning(joinPoint, "insight"))
					.doesNotThrowAnyException();
		}

		@Test
		@DisplayName("accepts a null result")
		void acceptsNullResult() {
			assertThatCode(() -> aspect.logAfterReturning(joinPoint, null))
					.doesNotThrowAnyException();
		}
	}
}
