package com.pgs.device.service.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Slf4j
public class ExecutionTimeAspect {

    @Pointcut("execution(* com.pgs.device.service.controller.*.*(..))")
    public void controllerMethods() {}

    /**
     * Measures and logs the execution time of controller methods matched by
     * {@link #controllerMethods()}.
     * <p>
     * Records the start time, proceeds with the intercepted method, and logs
     * the elapsed time in milliseconds regardless of whether the method
     * completes normally or throws an exception.
     *
     * @param pjp the proceeding join point representing the intercepted method
     * @return the result of the intercepted method
     * @throws Throwable if the intercepted method throws an exception
     */
    @Around("controllerMethods()")
    public Object measureExecutionTime(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.nanoTime();
        try {
            return pjp.proceed();
        } finally {
            long end = System.nanoTime();
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(end - start);
            String signature = pjp.getSignature().toShortString();
            log.info("Controller method {} executed in {} ms", signature, elapsedMs);
        }
    }
}
