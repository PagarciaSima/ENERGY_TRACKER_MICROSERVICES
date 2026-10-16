package com.pgs.alert.service.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Aspect that measures and logs the execution time of service-layer methods.
 * <p>
 * In this service there are no REST controllers (the entry point is the Kafka
 * listener), so the time-tracking aspect targets the service package instead,
 * matching the same convention used by the other microservices.
 */
@Aspect
@Component
@Slf4j
public class ExecutionTimeAspect {

    @Pointcut("execution(* com.pgs.alert.service.service.*.*(..))")
    public void serviceMethods() {
    }

    /**
     * Measures and logs the execution time of service methods matched by
     * {@link #serviceMethods()}.
     * <p>
     * Records the start time, proceeds with the intercepted method, and logs
     * the elapsed time in milliseconds regardless of whether the method
     * completes normally or throws an exception.
     *
     * @param pjp the proceeding join point representing the intercepted method
     * @return the result of the intercepted method
     * @throws Throwable if the intercepted method throws an exception
     */
    @Around("serviceMethods()")
    public Object measureExecutionTime(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.nanoTime();
        try {
            return pjp.proceed();
        } finally {
            long end = System.nanoTime();
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(end - start);
            String signature = pjp.getSignature().toShortString();
            log.info("Service method {} executed in {} ms", signature, elapsedMs);
        }
    }
}