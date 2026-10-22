package com.pgs.insight.service.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ExecutionTimeAspect}.
 *
 * <p>
 * The {@link ProceedingJoinPoint} is mocked so the aspect can be exercised in
 * isolation, without requiring a Spring context or an AOP proxy.
 */
@DisplayName("ExecutionTimeAspect")
class ExecutionTimeAspectTest {

	private ProceedingJoinPoint joinPoint;
	private ExecutionTimeAspect aspect;

	@BeforeEach
	void setUp() {
		joinPoint = mock(ProceedingJoinPoint.class);
		Signature signature = mock(Signature.class);
		when(signature.toShortString()).thenReturn("InsightController.getSavingTips(..)");
		when(joinPoint.getSignature()).thenReturn(signature);
		aspect = new ExecutionTimeAspect();
	}

	@Nested
	@DisplayName("measureExecutionTime")
	class MeasureExecutionTime {

		@Test
		@DisplayName("returns the result of the intercepted method")
		void returnsResultOfInterceptedMethod() throws Throwable {
			when(joinPoint.proceed()).thenReturn("result");

			Object result = aspect.measureExecutionTime(joinPoint);

			assertThat(result).isEqualTo("result");
		}

		@Test
		@DisplayName("propagates the exception thrown by the intercepted method")
		void propagatesExceptionOfInterceptedMethod() throws Throwable {
			when(joinPoint.proceed()).thenThrow(new IllegalStateException("boom"));

			assertThatThrownBy(() -> aspect.measureExecutionTime(joinPoint))
					.isInstanceOf(IllegalStateException.class)
					.hasMessage("boom");
		}

		@Test
		@DisplayName("returns null when the intercepted method returns null")
		void returnsNullWhenInterceptedMethodReturnsNull() throws Throwable {
			when(joinPoint.proceed()).thenReturn(null);

			assertThat(aspect.measureExecutionTime(joinPoint)).isNull();
		}
	}
}
