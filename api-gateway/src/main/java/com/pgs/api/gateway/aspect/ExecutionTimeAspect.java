package com.pgs.api.gateway.aspect;

import java.util.concurrent.TimeUnit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Aspect that measures and logs the execution time of the gateway route beans.
 */
@Aspect
@Component
@Slf4j
public class ExecutionTimeAspect {

    /**
     * Pointcut matching all methods of every class in the
     * {@code com.pgs.api.gateway.route} package.
     */
    @Pointcut("execution(* com.pgs.api.gateway.route.*.*(..))")
    public void routeMethods() {}

    /**
     * Measures and logs the execution time of route methods matched by
     * {@link #routeMethods()}.
     * <p>
     * Records the start time, proceeds with the intercepted method, and logs the
     * elapsed time in milliseconds regardless of whether the method completes
     * normally or throws an exception.
     *
     * @param pjp the proceeding join point representing the intercepted method
     * @return the result of the intercepted method
     * @throws Throwable if the intercepted method throws an exception
     */
    @Around("routeMethods()")
    public Object measureExecutionTime(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.nanoTime();
        try {
            return pjp.proceed();
        } finally {
            long end = System.nanoTime();
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(end - start);
            String signature = pjp.getSignature().toShortString();
            log.info("Route method {} executed in {} ms", signature, elapsedMs);
        }
    }
}
