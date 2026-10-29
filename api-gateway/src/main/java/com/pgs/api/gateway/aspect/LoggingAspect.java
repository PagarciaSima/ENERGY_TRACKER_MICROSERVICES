package com.pgs.api.gateway.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * Aspect that logs the invocation and successful return of the gateway's route
 * and configuration methods.
 * <p>
 * The gateway has no service or controller layer: its behaviour lives in the
 * {@code route} package (the {@code RouterFunction} beans) and in the
 * {@code config} package (security). Both are covered here so the gateway emits
 * the same kind of tracing as the other microservices.
 */
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    /**
     * Pointcut matching all methods of every class in the
     * {@code com.pgs.api.gateway.route} and {@code com.pgs.api.gateway.config}
     * packages.
     */
    @Pointcut("execution(* com.pgs.api.gateway.route.*.*(..)) "
            + "|| execution(* com.pgs.api.gateway.config.*.*(..))")
    public void gatewayMethods() {
    }

    /**
     * Logs the name and arguments of the intercepted method before it is invoked.
     *
     * @param joinPoint the intercepted method
     */
    @Before("gatewayMethods()")
    public void logBefore(JoinPoint joinPoint) {
        log.info("Called gateway method: {} with arguments: {}",
                joinPoint.getSignature().getName(), joinPoint.getArgs());
    }

    /**
     * Logs the name and returned value of the intercepted method after it completes
     * successfully. Not invoked if the method throws an exception.
     *
     * @param joinPoint the intercepted method
     * @param result    the value returned by the method
     */
    @AfterReturning(pointcut = "gatewayMethods()", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Object result) {
        log.info("Gateway method: {}, returned: {}",
                joinPoint.getSignature().getName(), result);
    }
}
